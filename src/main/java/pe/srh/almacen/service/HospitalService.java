package pe.srh.almacen.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.srh.almacen.dto.StockSedeFila;
import pe.srh.almacen.entity.*;
import pe.srh.almacen.repository.*;

/**
 * Módulo de sedes hospitalarias: cada hospital empieza con stock 0, confirma los envíos del almacén
 * y registra su consumo por turno. El stock del hospital se calcula así:
 * lo recibido (salidas confirmadas desde el inicio del control) + ajustes - consumos.
 */
@Service
public class HospitalService {

    // Tope de una cantidad registrada, para rechazar errores de digitación con un mensaje claro
    private static final int MAXIMO_CANTIDAD = MovimientoServiceImpl.MAXIMO_CANTIDAD;

    @Autowired private SedeRepository sedeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ConsumoSedeRepository consumoRepository;
    @Autowired private MovimientoRepository movimientoRepository;
    @Autowired private MovimientoDetalleRepository detalleRepository;
    @Autowired private RegistroErrorRepository errorRepository;

    /** Hospital del encargado que inició sesión. */
    @Transactional(readOnly = true)
    public Sede sedeDe(String username) {
        Usuario u = usuarioRepository.findByUsername(username).orElseThrow();
        if (u.getSede() == null) {
            throw new IllegalStateException("El usuario " + username + " no tiene un hospital asignado.");
        }
        return u.getSede();
    }

    /** Hospitales activos (para la vista del jefe). */
    @Transactional(readOnly = true)
    public List<Sede> hospitales() {
        return sedeRepository.findByTipoAndActivoTrueOrderByNombreAsc("HOSPITAL");
    }

    /** Stock de cada producto en el hospital, con lo que viene en camino y el consumo reciente. */
    @Transactional(readOnly = true)
    public List<StockSedeFila> stockDeSede(Sede sede) {
        if (!sede.isControlIniciado()) {
            return List.of();
        }
        Integer id = sede.getIdSede();
        LocalDateTime inicio = sede.getFechaInicioControl().atStartOfDay();
        Map<Integer, Integer> stock = new HashMap<>();
        for (Object[] f : detalleRepository.sumarRecibidoPorSede(id, inicio)) {
            stock.merge((Integer) f[0], ((Number) f[1]).intValue(), Integer::sum);
        }
        for (Object[] f : consumoRepository.sumarPorProductoYTipo(id)) {
            int cantidad = ((Number) f[2]).intValue();
            stock.merge((Integer) f[0], "CONSUMO".equals(f[1]) ? -cantidad : cantidad, Integer::sum);
        }
        Map<Integer, Integer> enCamino = new HashMap<>();
        for (Movimiento m : movimientoRepository.findEnviosPendientes(id, inicio)) {
            m.getDetalles().forEach(d -> enCamino.merge(d.getProducto().getIdProducto(), d.getCantidad(), Integer::sum));
        }
        Map<Integer, Integer> consumo7 = sumas(consumoRepository.sumarConsumoDesde(id, LocalDateTime.now().minusDays(7)));
        Map<Integer, Integer> consumo30 = sumas(consumoRepository.sumarConsumoDesde(id, LocalDateTime.now().minusDays(30)));

        List<StockSedeFila> filas = new ArrayList<>();
        for (Producto p : productoRepository.findAllByOrderByCodigoAsc()) {
            int s = stock.getOrDefault(p.getIdProducto(), 0);
            int c = enCamino.getOrDefault(p.getIdProducto(), 0);
            // Se muestran los productos activos y los inactivos que aún tienen saldo en el hospital
            if (p.getActivo() || s != 0 || c != 0) {
                filas.add(new StockSedeFila(p, s, c, consumo7.getOrDefault(p.getIdProducto(), 0),
                        consumo30.getOrDefault(p.getIdProducto(), 0)));
            }
        }
        return filas;
    }

    /** Stock de un solo producto en el hospital. */
    @Transactional(readOnly = true)
    public int stockDeProducto(Sede sede, Integer idProducto) {
        return stockDeSede(sede).stream().filter(f -> f.getProducto().getIdProducto().equals(idProducto))
                .mapToInt(StockSedeFila::getStock).findFirst().orElse(0);
    }

    /**
     * Los hospitales sin fecha de inicio empiezan hoy con todo en 0 (se llama al arrancar el sistema).
     * Los envíos anteriores a ese día no cuentan para su stock.
     *
     * @return cuántos hospitales se iniciaron
     */
    @Transactional
    public int iniciarHospitalesPendientes() {
        int iniciados = 0;
        for (Sede s : sedeRepository.findByTipoAndActivoTrueOrderByNombreAsc("HOSPITAL")) {
            if (!s.isControlIniciado()) {
                s.setFechaInicioControl(LocalDate.now());
                iniciados++;
            }
        }
        return iniciados;
    }

    /** CU-12: consumo del turno. Baja el stock del hospital; no puede pasar lo que hay. */
    @Transactional
    public ConsumoSede registrarConsumo(Integer idSede, Integer idProducto, Integer cantidad, String username) {
        // Se bloquea la sede para que dos registros a la vez no consuman más de lo que hay
        Sede sede = sedeRepository.findParaActualizar(idSede).orElseThrow();
        List<String> errores = new ArrayList<>();
        if (!sede.getActivo()) {
            throw new MovimientoException(List.of("El hospital está desactivado: no puede registrar consumos."));
        }
        if (cantidad != null && cantidad > MAXIMO_CANTIDAD) {
            errores.add("La cantidad no puede pasar de " + MAXIMO_CANTIDAD + ".");
        }
        if (idProducto == null) {
            errores.add("Seleccione el producto.");
        }
        if (cantidad == null || cantidad < 1) {
            errores.add("La cantidad usada debe ser mayor a 0.");
        }
        if (!errores.isEmpty()) {
            throw new MovimientoException(errores);
        }
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new MovimientoException(List.of("El producto no existe.")));
        int disponible = stockDeProducto(sede, idProducto);
        if (cantidad > disponible) {
            throw new MovimientoException(List.of(p.getCodigo() + " " + p.getNombre() + ": quiere registrar "
                    + cantidad + " y en el hospital solo hay " + disponible + " " + p.getUnidad() + "."));
        }
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        return consumoRepository.save(nuevoRegistro(sede, p, "CONSUMO", cantidad,
                turnoPara(LocalTime.now()), null, usuario));
    }

    /** Envíos del almacén que el hospital aún no confirma. */
    @Transactional(readOnly = true)
    public List<Movimiento> enviosPendientes(Sede sede) {
        if (!sede.isControlIniciado()) {
            return List.of();
        }
        return movimientoRepository.findEnviosPendientes(sede.getIdSede(), sede.getFechaInicioControl().atStartOfDay());
    }

    /**
     * CU-13: confirmar un envío. Sin cantidades (o si todo coincide) queda CONFORME. Si llegó menos,
     * queda DIFERENTE: se descuenta lo que no llegó del stock del hospital y se reporta al jefe.
     *
     * @param llegoPorDetalle cantidad que llegó por cada detalle del envío, o null si llegó todo
     * @return true si el envío quedó conforme, false si se reportó una diferencia
     */
    @Transactional
    public boolean confirmarEnvio(Integer idSede, Integer idMovimiento, Map<Integer, Integer> llegoPorDetalle,
                                  String username) {
        Sede sede = sedeRepository.findParaActualizar(idSede).orElseThrow();
        if (!sede.getActivo()) {
            throw new MovimientoException(List.of("El hospital está desactivado: no puede confirmar envíos."));
        }
        Movimiento envio = movimientoRepository.findConDetalle(idMovimiento).orElse(null);
        if (envio == null || !"SALIDA".equals(envio.getTipo()) || envio.getSede() == null
                || !envio.getSede().getIdSede().equals(idSede) || !"ACTIVO".equals(envio.getEstado())) {
            throw new MovimientoException(List.of("Ese envío no corresponde a su hospital."));
        }
        if (envio.getConfirmadoSede() != null) {
            throw new MovimientoException(List.of("Este envío ya fue confirmado."));
        }
        if (!sede.isControlIniciado() || envio.getFecha().isBefore(sede.getFechaInicioControl().atStartOfDay())) {
            throw new MovimientoException(List.of("Este envío es anterior al inicio del control del hospital."));
        }

        List<String> errores = new ArrayList<>();
        List<MovimientoDetalle> conDiferencia = new ArrayList<>();
        if (llegoPorDetalle != null) {
            for (MovimientoDetalle d : envio.getDetalles()) {
                Integer llego = llegoPorDetalle.get(d.getIdDetalle());
                if (llego == null || llego < 0) {
                    errores.add(d.getProducto().getCodigo() + ": indique cuántas unidades llegaron (0 si no llegó nada).");
                } else if (llego > d.getCantidad()) {
                    errores.add(d.getProducto().getCodigo() + ": no pueden llegar más de las " + d.getCantidad()
                            + " despachadas.");
                } else if (llego < d.getCantidad()) {
                    conDiferencia.add(d);
                }
            }
        }
        if (!errores.isEmpty()) {
            throw new MovimientoException(errores);
        }

        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        for (MovimientoDetalle d : conDiferencia) {
            int llego = llegoPorDetalle.get(d.getIdDetalle());
            String texto = d.getProducto().getCodigo() + " " + d.getProducto().getNombre() + ": se despacharon "
                    + d.getCantidad() + " y llegaron " + llego;
            // Lo que no llegó no forma parte del stock del hospital
            ConsumoSede ajuste = consumoRepository.save(nuevoRegistro(sede, d.getProducto(), "AJUSTE",
                    llego - d.getCantidad(), null, "Diferencia en envío N.° " + envio.getIdMovimiento(), usuario));
            RegistroError e = new RegistroError();
            e.setMovimiento(envio);
            e.setConsumo(ajuste);   // el ajuste que descontó lo que no llegó
            e.setAccion("DIFERENCIA");
            e.setValorAnterior(d.getCantidad());
            e.setValorNuevo(llego);
            e.setMotivo("ENVIO");
            e.setDetalle(texto.length() > 200 ? texto.substring(0, 200) : texto);
            e.setUsuarioReporta(usuario);
            e.setFechaReporte(LocalDateTime.now());
            e.setEstado("PENDIENTE");
            errorRepository.save(e);
        }
        envio.setConfirmadoSede(conDiferencia.isEmpty() ? "CONFORME" : "DIFERENTE");
        movimientoRepository.save(envio);
        return conDiferencia.isEmpty();
    }

    /** Últimos registros del hospital (consumos y diferencias de envío). */
    @Transactional(readOnly = true)
    public List<ConsumoSede> ultimosRegistros(Sede sede) {
        return consumoRepository.findTop15BySedeIdSedeOrderByFechaDescIdConsumoDesc(sede.getIdSede());
    }

    /** Diferencias de envío reportadas por los hospitales que el jefe aún no revisa. */
    @Transactional(readOnly = true)
    public List<RegistroError> diferenciasPendientes() {
        return errorRepository.findByEstadoOrderByFechaReporteDesc("PENDIENTE");
    }

    /** Diferencias de envío de un hospital que el jefe aún no revisa. */
    @Transactional(readOnly = true)
    public List<RegistroError> diferenciasPendientes(Sede sede) {
        return errorRepository.findByMovimientoSedeIdSedeAndEstadoOrderByFechaReporteDesc(sede.getIdSede(), "PENDIENTE");
    }

    /**
     * El jefe cierra una diferencia de envío: queda APROBADO o RECHAZADO con quién la revisó y cuándo.
     * La revisión no cambia el stock del hospital (lo que no llegó ya se descontó al confirmar el envío).
     *
     * @return la diferencia revisada
     */
    @Transactional
    public RegistroError revisarDiferencia(Integer idError, String estado, String username) {
        if (!"APROBADO".equals(estado) && !"RECHAZADO".equals(estado)) {
            throw new MovimientoException(List.of("Elija si la diferencia se aprueba o se rechaza."));
        }
        RegistroError e = errorRepository.findById(idError)
                .orElseThrow(() -> new MovimientoException(List.of("La diferencia no existe.")));
        if (!"PENDIENTE".equals(e.getEstado())) {
            throw new MovimientoException(List.of("Esta diferencia ya fue revisada."));
        }
        e.setEstado(estado);
        e.setUsuarioRevisa(usuarioRepository.findByUsername(username).orElseThrow());
        e.setFechaRevision(LocalDateTime.now());
        return errorRepository.save(e);
    }

    /** Turno según la hora: mañana de 6 a 14, tarde de 14 a 22 y noche de 22 a 6. */
    public static String turnoPara(LocalTime hora) {
        int h = hora.getHour();
        if (h >= 6 && h < 14) return "MANANA";
        if (h >= 14 && h < 22) return "TARDE";
        return "NOCHE";
    }

    private ConsumoSede nuevoRegistro(Sede sede, Producto p, String tipo, int cantidad, String turno, String motivo,
                                      Usuario usuario) {
        ConsumoSede c = new ConsumoSede();
        c.setSede(sede);
        c.setProducto(p);
        c.setFecha(LocalDateTime.now());
        c.setTipo(tipo);
        c.setCantidad(cantidad);
        c.setTurno(turno);
        c.setMotivo(motivo);
        c.setUsuario(usuario);
        c.setEstado("ACTIVO");
        return c;
    }

    private Map<Integer, Integer> sumas(List<Object[]> filas) {
        Map<Integer, Integer> m = new HashMap<>();
        for (Object[] f : filas) {
            m.put((Integer) f[0], ((Number) f[1]).intValue());
        }
        return m;
    }
}
