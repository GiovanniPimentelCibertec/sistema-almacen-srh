package pe.srh.almacen.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.srh.almacen.dto.AjusteForm;
import pe.srh.almacen.dto.MovimientoForm;
import pe.srh.almacen.dto.MovimientoForm.Linea;
import pe.srh.almacen.entity.Movimiento;
import pe.srh.almacen.entity.MovimientoDetalle;
import pe.srh.almacen.entity.Producto;
import pe.srh.almacen.entity.Sede;
import pe.srh.almacen.repository.MovimientoDetalleRepository;
import pe.srh.almacen.repository.MovimientoRepository;
import pe.srh.almacen.repository.ProductoRepository;
import pe.srh.almacen.repository.SedeRepository;
import pe.srh.almacen.repository.UsuarioRepository;

@Service
public class MovimientoServiceImpl implements MovimientoService {

    // Días de consumo que se usan para calcular el stock mínimo (tope; si hay menos historia se usan esos días)
    private static final int DIAS_CONSUMO = 90;

    // Tope de una cantidad por renglón: evita desbordes y errores de digitación (por ejemplo 99999999)
    static final int MAXIMO_CANTIDAD = 1_000_000;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private MovimientoDetalleRepository detalleRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SedeRepository sedeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Movimiento> listar(String tipo) {
        return movimientoRepository.findPorTipoConDetalle(tipo);
    }

    @Override
    @Transactional(readOnly = true)
    public Movimiento buscarConDetalle(Integer id) {
        return movimientoRepository.findConDetalle(id).orElse(null);
    }

    @Override
    @Transactional
    public Movimiento registrarSalida(MovimientoForm form, String username) {
        List<String> errores = new ArrayList<>();
        Sede sede = null;
        if (form.getIdSede() == null) {
            errores.add("Seleccione la sede de destino.");
        } else {
            sede = sedeRepository.findById(form.getIdSede()).orElse(null);
            if (sede == null || !sede.getActivo()) {
                errores.add("La sede elegida no existe o está desactivada.");
            }
        }
        List<Linea> lineas = validarLineas(form, true, errores);

        // Bloquear los productos y validar que alcance el stock
        Map<Integer, Producto> productos = bloquearProductos(lineas, errores);
        for (Linea l : lineas) {
            Producto p = productos.get(l.getIdProducto());
            if (p != null && l.getCantidad() > p.getStock()) {
                errores.add(p.getCodigo() + " " + p.getNombre() + ": se quiere despachar " + l.getCantidad()
                        + " y solo hay " + p.getStock() + " en stock.");
            }
        }
        if (!errores.isEmpty()) {
            throw new MovimientoException(errores);
        }

        // Guardar la cabecera, un detalle por producto y descontar el stock
        Movimiento salida = nuevoMovimiento("SALIDA", sede, form.getObservacion(), username);
        for (Linea l : lineas) {
            Producto p = productos.get(l.getIdProducto());
            p.setStock(p.getStock() - l.getCantidad());
            agregarDetalle(salida, p, l.getCantidadPedida(), l.getCantidad());
        }
        movimientoRepository.saveAndFlush(salida);

        // Recalcular el stock mínimo con el consumo real, que ya incluye esta salida
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime desde = ahora.minusDays(DIAS_CONSUMO);
        for (Producto p : productos.values()) {
            long despachado = detalleRepository.sumarSalidasDesde(p.getIdProducto(), desde);
            int dias = diasDeHistoria(detalleRepository.primeraSalida(p.getIdProducto()), ahora.toLocalDate());
            p.setStockMinimo(calcularStockMinimo(despachado, dias, p));
        }
        return salida;
    }

    @Override
    @Transactional
    public Movimiento registrarEntrada(MovimientoForm form, String username) {
        List<String> errores = new ArrayList<>();
        List<Linea> lineas = validarLineas(form, false, errores);
        Map<Integer, Producto> productos = bloquearProductos(lineas, errores);
        if (!errores.isEmpty()) {
            throw new MovimientoException(errores);
        }

        // Las entradas no tienen sede: la mercadería llega al almacén central
        Movimiento entrada = nuevoMovimiento("ENTRADA", null, form.getObservacion(), username);
        for (Linea l : lineas) {
            Producto p = productos.get(l.getIdProducto());
            p.setStock(p.getStock() + l.getCantidad());
            agregarDetalle(entrada, p, null, l.getCantidad());
        }
        movimientoRepository.saveAndFlush(entrada);
        return entrada;
    }

    @Override
    @Transactional
    public Movimiento registrarAjuste(AjusteForm form, String username) {
        List<String> errores = new ArrayList<>();
        String motivo = form.getMotivo() == null ? "" : form.getMotivo().trim();
        if (form.getIdProducto() == null) {
            errores.add("Seleccione el producto.");
        }
        if (form.getCantidad() == null || form.getCantidad() == 0) {
            errores.add("Ingrese la cantidad a sumar (positiva) o a restar (negativa); no puede ser 0.");
        } else if (Math.abs(form.getCantidad()) > MAXIMO_CANTIDAD) {
            errores.add("La cantidad no puede pasar de " + MAXIMO_CANTIDAD + ".");
        }
        if (motivo.isEmpty()) {
            errores.add("Ingrese el motivo del ajuste.");
        } else if (motivo.length() > 200) {
            errores.add("El motivo no puede pasar de 200 caracteres.");
        }
        Producto p = null;
        if (errores.isEmpty()) {
            p = productoRepository.findParaActualizar(form.getIdProducto()).orElse(null);
            if (p == null || !p.getActivo()) {
                errores.add("El producto elegido no existe o está desactivado.");
            } else if (p.getStock() + form.getCantidad() < 0) {
                errores.add(p.getCodigo() + " " + p.getNombre() + ": el ajuste dejaría el stock en negativo (hay "
                        + p.getStock() + " y se quieren restar " + (-form.getCantidad()) + ").");
            }
        }
        if (!errores.isEmpty()) {
            throw new MovimientoException(errores);
        }

        Movimiento ajuste = nuevoMovimiento("AJUSTE", null, motivo, username);
        p.setStock(p.getStock() + form.getCantidad());
        agregarDetalle(ajuste, p, null, form.getCantidad());   // la cantidad se guarda con su signo
        movimientoRepository.saveAndFlush(ajuste);
        return ajuste;
    }

    @Override
    public List<Producto> productosPorReponer(Movimiento movimiento) {
        return movimiento.getDetalles().stream()
                .map(MovimientoDetalle::getProducto)
                .filter(p -> p.getStock() <= p.getStockMinimo())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoDetalle> kardex(Integer idProducto, LocalDate desde, LocalDate hasta) {
        return detalleRepository.findKardex(idProducto, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    // ---------- Ayudantes ----------

    /** Valida los renglones del formulario y devuelve solo los que el usuario llenó. */
    private List<Linea> validarLineas(MovimientoForm form, boolean esSalida, List<String> errores) {
        if (form.getObservacion() != null && form.getObservacion().length() > 200) {
            errores.add("La observación no puede pasar de 200 caracteres.");
        }
        List<Linea> lineas = form.getLineas().stream().filter(l -> l != null && !l.isVacia()).toList();
        if (lineas.isEmpty()) {
            errores.add("Agregue al menos un producto.");
        }
        Set<Integer> vistos = new HashSet<>();
        int total = 0;
        for (int i = 0; i < lineas.size(); i++) {
            Linea l = lineas.get(i);
            String renglon = "Renglón " + (i + 1) + ": ";
            if (l.getIdProducto() == null) {
                errores.add(renglon + "seleccione el producto.");
                continue;
            }
            if (!vistos.add(l.getIdProducto())) {
                errores.add(renglon + "el producto está repetido; júntelo en un solo renglón.");
            }
            if ((l.getCantidad() != null && l.getCantidad() > MAXIMO_CANTIDAD)
                    || (l.getCantidadPedida() != null && l.getCantidadPedida() > MAXIMO_CANTIDAD)) {
                errores.add(renglon + "la cantidad no puede pasar de " + MAXIMO_CANTIDAD + ".");
                continue;
            }
            if (esSalida) {
                if (l.getCantidadPedida() == null || l.getCantidadPedida() < 1) {
                    errores.add(renglon + "la cantidad pedida debe ser mayor a 0.");
                }
                if (l.getCantidad() == null || l.getCantidad() < 0) {
                    errores.add(renglon + "ingrese la cantidad despachada (0 si no se envió nada).");
                } else {
                    total += l.getCantidad();
                    if (l.getCantidadPedida() != null && l.getCantidad() > l.getCantidadPedida()) {
                        errores.add(renglon + "no se puede despachar más de lo pedido.");
                    }
                }
            } else if (l.getCantidad() == null || l.getCantidad() < 1) {
                errores.add(renglon + "la cantidad recibida debe ser mayor a 0.");
            }
        }
        if (esSalida && errores.isEmpty() && total == 0) {
            errores.add("Debe despachar al menos una unidad.");
        }
        if (!errores.isEmpty()) {
            throw new MovimientoException(errores);
        }
        return lineas;
    }

    /** Bloquea los productos en orden de id (evita bloqueos cruzados) y verifica que estén activos. */
    private Map<Integer, Producto> bloquearProductos(List<Linea> lineas, List<String> errores) {
        Map<Integer, Producto> productos = new HashMap<>();
        lineas.stream().map(Linea::getIdProducto).sorted(Comparator.naturalOrder()).forEach(id ->
                productoRepository.findParaActualizar(id).ifPresent(p -> productos.put(id, p)));
        for (Linea l : lineas) {
            Producto p = productos.get(l.getIdProducto());
            if (p == null || !p.getActivo()) {
                errores.add("Un producto elegido no existe o está desactivado.");
                productos.remove(l.getIdProducto());
            }
        }
        return productos;
    }

    private Movimiento nuevoMovimiento(String tipo, Sede sede, String observacion, String username) {
        Movimiento m = new Movimiento();
        m.setTipo(tipo);
        m.setFecha(LocalDateTime.now());
        m.setSede(sede);
        m.setUsuario(usuarioRepository.findByUsername(username).orElseThrow());
        m.setEstado("ACTIVO");
        String obs = observacion == null ? "" : observacion.trim();
        m.setObservacion(obs.isEmpty() ? null : obs);
        return m;
    }

    private void agregarDetalle(Movimiento m, Producto p, Integer cantidadPedida, int cantidad) {
        MovimientoDetalle d = new MovimientoDetalle();
        d.setMovimiento(m);
        d.setProducto(p);
        d.setCantidadPedida(cantidadPedida);
        d.setCantidad(cantidad);
        d.setStockResultante(p.getStock());
        m.getDetalles().add(d);
    }

    /**
     * Días de historia para el promedio diario: los días desde la primera salida activa del producto,
     * con un tope de 90 y como mínimo 1. Así un producto nuevo no divide su consumo entre 90 días que no tuvo.
     */
    static int diasDeHistoria(LocalDateTime primeraSalida, LocalDate hoy) {
        if (primeraSalida == null) {
            return 1;
        }
        long dias = ChronoUnit.DAYS.between(primeraSalida.toLocalDate(), hoy);
        return (int) Math.max(1, Math.min(DIAS_CONSUMO, dias));
    }

    /** stock_minimo = redondear hacia arriba((salidas de los últimos 90 días / días de historia) × días de reposición). */
    static int calcularStockMinimo(long despachado, int diasHistoria, Producto p) {
        if (despachado == 0) {
            return p.getMinimoInicial();
        }
        return (int) Math.ceil((double) despachado / diasHistoria * p.getDiasReposicion());
    }
}
