package pe.srh.almacen.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pe.srh.almacen.dto.AjusteForm;
import pe.srh.almacen.dto.MovimientoForm;
import pe.srh.almacen.entity.*;
import pe.srh.almacen.repository.*;

/** Pruebas de entradas, salidas (caso transaccional), ajustes, kardex y reportes. */
@SpringBootTest
class MovimientoServiceTest {

    @Autowired private MovimientoService movimientoService;
    @Autowired private ReporteService reporteService;
    @Autowired private MovimientoRepository movimientoRepository;
    @Autowired private MovimientoDetalleRepository detalleRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private SedeRepository sedeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private MarcaRepository marcaRepository;

    private Sede barton;
    private Producto bolsa;     // stock 100, 9 días de reposición, mínimo inicial 5
    private Producto guantes;   // stock 10

    @BeforeEach
    void prepararDatos() {
        Categoria cat = new Categoria();
        cat.setNombre("Bolsas");
        categoriaRepository.save(cat);
        Marca marca = new Marca();
        marca.setNombre("Genérica");
        marcaRepository.save(marca);

        barton = new Sede();
        barton.setNombre("Hospital Barton");
        barton.setTipo("HOSPITAL");
        sedeRepository.save(barton);

        Usuario jefe = new Usuario();
        jefe.setNombres("Jefe de logística");
        jefe.setUsername("jefe");
        jefe.setPassword("x");
        jefe.setRol("JEFE");
        usuarioRepository.save(jefe);

        bolsa = crearProducto("BOL-001", "Bolsa roja 60x90", 100, cat, marca);
        guantes = crearProducto("EPP-002", "Guantes nitrilo", 10, cat, marca);
    }

    @AfterEach
    void limpiar() {
        detalleRepository.deleteAll();
        movimientoRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        sedeRepository.deleteAll();
        categoriaRepository.deleteAll();
        marcaRepository.deleteAll();
    }

    // ---------- Salidas (caso transaccional) ----------

    @Test
    void salidaCorrectaDescuentaStockYGuardaTodo() {
        Movimiento salida = movimientoService.registrarSalida(salida(linea(bolsa, 30, 30), linea(guantes, 4, 4)), "jefe");

        assertEquals("SALIDA", salida.getTipo());
        assertEquals(2, salida.getDetalles().size());
        assertFalse(salida.isEntregaIncompleta());
        assertEquals(70, stockDe(bolsa));
        assertEquals(6, stockDe(guantes));
    }

    @Test
    void salidaSinStockSuficienteNoGuardaNada() {
        MovimientoException error = assertThrows(MovimientoException.class,
                () -> movimientoService.registrarSalida(salida(linea(bolsa, 30, 30), linea(guantes, 15, 15)), "jefe"));

        assertTrue(error.getMessage().contains("EPP-002"), "El error debe indicar el producto con problema");
        assertEquals(100, stockDe(bolsa), "El stock de la bolsa no debe cambiar");
        assertEquals(10, stockDe(guantes));
        assertEquals(0, movimientoRepository.count());
    }

    @Test
    void despacharMenosDeLoPedidoEsEntregaIncompleta() {
        Movimiento salida = movimientoService.registrarSalida(salida(linea(guantes, 15, 10)), "jefe");

        assertTrue(salida.isEntregaIncompleta());
        assertEquals(0, stockDe(guantes));
    }

    @Test
    void salidaRecalculaElStockMinimoConElConsumoReal() {
        // El producto ya tiene más de 90 días de historia (una salida antigua que no entra en la suma)
        Movimiento antigua = movimientoService.registrarSalida(salida(linea(bolsa, 1, 1)), "jefe");
        antigua.setFecha(LocalDateTime.now().minusDays(100));
        movimientoRepository.save(antigua);

        // 90 unidades en 90 días = 1 por día; con 9 días de reposición el mínimo es 9
        movimientoService.registrarSalida(salida(linea(bolsa, 90, 90)), "jefe");

        assertEquals(9, productoRepository.findById(bolsa.getIdProducto()).orElseThrow().getStockMinimo());
    }

    @Test
    void conMenosDe90DiasDeHistoriaSeDivideEntreLosDiasReales() {
        // Primera salida hace 30 días: 30 unidades
        Movimiento primera = movimientoService.registrarSalida(salida(linea(bolsa, 30, 30)), "jefe");
        primera.setFecha(LocalDateTime.now().minusDays(30));
        movimientoRepository.save(primera);

        // Hoy salen 30 más: 60 unidades en 30 días = 2 por día; x 9 días de reposición = 18 (antes daba 6)
        movimientoService.registrarSalida(salida(linea(bolsa, 30, 30)), "jefe");

        assertEquals(18, productoRepository.findById(bolsa.getIdProducto()).orElseThrow().getStockMinimo());
    }

    @Test
    void laPrimeraSalidaDelDiaCuentaComoUnDiaDeHistoria() {
        // Producto sin historia: 3 unidades hoy / 1 día x 9 días de reposición = 27
        movimientoService.registrarSalida(salida(linea(guantes, 3, 3)), "jefe");

        assertEquals(27, productoRepository.findById(guantes.getIdProducto()).orElseThrow().getStockMinimo());
    }

    @Test
    void losDiasDeHistoriaVanDe1A90() {
        LocalDate hoy = LocalDate.of(2026, 10, 20);
        assertEquals(1, MovimientoServiceImpl.diasDeHistoria(null, hoy), "Sin salidas");
        assertEquals(1, MovimientoServiceImpl.diasDeHistoria(LocalDateTime.of(2026, 10, 20, 8, 0), hoy), "Hoy: mínimo 1");
        assertEquals(29, MovimientoServiceImpl.diasDeHistoria(LocalDateTime.of(2026, 9, 21, 10, 9), hoy));
        assertEquals(90, MovimientoServiceImpl.diasDeHistoria(LocalDateTime.of(2026, 1, 1, 0, 0), hoy), "Tope de 90");

        // Datos de la demo: Mascarilla KN95, 1790 despachadas en 29 días x 10 días de reposición = 617.2 -> 618
        Producto kn95 = new Producto();
        kn95.setDiasReposicion(10);
        kn95.setMinimoInicial(60);
        assertEquals(618, MovimientoServiceImpl.calcularStockMinimo(1790, 29, kn95));
        assertEquals(60, MovimientoServiceImpl.calcularStockMinimo(0, 29, kn95), "Sin salidas: mínimo inicial");
    }

    @Test
    void lasSalidasDeHaceMasDe90DiasNoCuentanParaElMinimo() {
        Movimiento antigua = movimientoService.registrarSalida(salida(linea(bolsa, 45, 45)), "jefe");
        antigua.setFecha(LocalDateTime.now().minusDays(120));
        movimientoRepository.save(antigua);

        // Solo cuenta esta: 9 unidades en 90 días x 9 días = 0.9, redondeado hacia arriba = 1
        movimientoService.registrarSalida(salida(linea(bolsa, 9, 9)), "jefe");

        assertEquals(1, productoRepository.findById(bolsa.getIdProducto()).orElseThrow().getStockMinimo());
    }

    @Test
    void salidaValidaLosDatosDelFormulario() {
        MovimientoForm sinSede = salida(linea(bolsa, 5, 5));
        sinSede.setIdSede(null);
        assertThrows(MovimientoException.class, () -> movimientoService.registrarSalida(sinSede, "jefe"));
        assertThrows(MovimientoException.class,
                () -> movimientoService.registrarSalida(salida(linea(bolsa, 5, 5), linea(bolsa, 2, 2)), "jefe"));
        assertThrows(MovimientoException.class,
                () -> movimientoService.registrarSalida(salida(linea(bolsa, 5, 8)), "jefe"));
        assertThrows(MovimientoException.class,
                () -> movimientoService.registrarSalida(salida(linea(bolsa, 5, 0)), "jefe"));
        assertThrows(MovimientoException.class, () -> movimientoService.registrarSalida(salida(), "jefe"));

        assertEquals(0, movimientoRepository.count());
        assertEquals(100, stockDe(bolsa));
    }

    // ---------- Entradas ----------

    @Test
    void entradaSumaElStockDeVariosProductos() {
        Movimiento entrada = movimientoService.registrarEntrada(entrada(linea(bolsa, null, 50), linea(guantes, null, 20)), "jefe");

        assertEquals("ENTRADA", entrada.getTipo());
        assertNull(entrada.getSede(), "Las entradas no tienen sede");
        assertEquals(150, stockDe(bolsa));
        assertEquals(30, stockDe(guantes));
        assertEquals(150, entrada.getDetalles().get(0).getStockResultante());
    }

    @Test
    void entradaConCantidadCeroNoGuardaNada() {
        assertThrows(MovimientoException.class,
                () -> movimientoService.registrarEntrada(entrada(linea(bolsa, null, 50), linea(guantes, null, 0)), "jefe"));

        assertEquals(100, stockDe(bolsa));
        assertEquals(0, movimientoRepository.count());
    }

    @Test
    void entradaDeProductoDesactivadoEsRechazada() {
        guantes.setActivo(false);
        productoRepository.save(guantes);

        assertThrows(MovimientoException.class,
                () -> movimientoService.registrarEntrada(entrada(linea(guantes, null, 5)), "jefe"));
        assertEquals(10, stockDe(guantes));
    }

    // ---------- Ajustes ----------

    @Test
    void ajustePositivoYNegativoCorrigenElStock() {
        movimientoService.registrarAjuste(ajuste(bolsa, 5, "Sobrante en conteo físico"), "jefe");
        assertEquals(105, stockDe(bolsa));

        Movimiento m = movimientoService.registrarAjuste(ajuste(bolsa, -20, "Faltante en conteo físico"), "jefe");
        assertEquals(85, stockDe(bolsa));
        assertEquals(-20, m.getDetalles().get(0).getCantidad(), "La cantidad se guarda con su signo");
        assertEquals("Faltante en conteo físico", m.getObservacion());
    }

    @Test
    void ajusteNoPuedeDejarStockNegativo() {
        assertThrows(MovimientoException.class,
                () -> movimientoService.registrarAjuste(ajuste(guantes, -11, "Conteo"), "jefe"));
        assertEquals(10, stockDe(guantes));
    }

    @Test
    void ajusteExigeMotivoYCantidadDistintaDeCero() {
        assertThrows(MovimientoException.class, () -> movimientoService.registrarAjuste(ajuste(bolsa, 5, " "), "jefe"));
        assertThrows(MovimientoException.class, () -> movimientoService.registrarAjuste(ajuste(bolsa, 0, "Conteo"), "jefe"));
        assertEquals(0, movimientoRepository.count());
    }

    // ---------- Kardex ----------

    @Test
    void kardexMuestraLosMovimientosEnOrdenConSuSaldo() {
        movimientoService.registrarEntrada(entrada(linea(bolsa, null, 50)), "jefe");
        movimientoService.registrarSalida(salida(linea(bolsa, 30, 30)), "jefe");
        movimientoService.registrarAjuste(ajuste(bolsa, -5, "Conteo"), "jefe");
        movimientoService.registrarSalida(salida(linea(guantes, 2, 2)), "jefe");   // otro producto: no aparece

        List<MovimientoDetalle> filas = movimientoService.kardex(bolsa.getIdProducto(), LocalDate.now(), LocalDate.now());

        assertEquals(List.of("ENTRADA", "SALIDA", "AJUSTE"), filas.stream().map(d -> d.getMovimiento().getTipo()).toList());
        assertEquals(List.of(150, 120, 115), filas.stream().map(MovimientoDetalle::getStockResultante).toList());
        assertTrue(movimientoService.kardex(bolsa.getIdProducto(), LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1)).isEmpty(), "Fuera del rango no hay movimientos");
    }

    // ---------- Reportes ----------

    @Test
    void reportesGeneranPdf() {
        movimientoService.registrarSalida(salida(linea(bolsa, 30, 25), linea(guantes, 10, 10)), "jefe");

        byte[] consumo = reporteService.consumoPorSede(YearMonth.now(), "jefe");
        byte[] reponer = reporteService.productosPorReponer("jefe");
        byte[] sinDatos = reporteService.consumoPorSede(YearMonth.now().minusYears(5), "jefe");

        for (byte[] pdf : List.of(consumo, reponer, sinDatos)) {
            assertTrue(pdf.length > 500, "El PDF no debe estar vacío");
            assertEquals("%PDF", new String(pdf, 0, 4), "Debe ser un archivo PDF");
        }
    }

    // ---------- Ayudantes ----------

    private Producto crearProducto(String codigo, String nombre, int stock, Categoria cat, Marca marca) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setUnidad("UND");
        p.setStock(stock);
        p.setStockMinimo(5);
        p.setMinimoInicial(5);
        p.setDiasReposicion(9);
        p.setCategoria(cat);
        p.setMarca(marca);
        return productoRepository.save(p);
    }

    private MovimientoForm salida(MovimientoForm.Linea... lineas) {
        MovimientoForm form = entrada(lineas);
        form.setIdSede(barton.getIdSede());
        form.setObservacion("Pedido por WhatsApp");
        return form;
    }

    private MovimientoForm entrada(MovimientoForm.Linea... lineas) {
        MovimientoForm form = new MovimientoForm();
        form.setObservacion("Guía de remisión 001");
        form.getLineas().addAll(List.of(lineas));
        return form;
    }

    private MovimientoForm.Linea linea(Producto p, Integer pedida, int cantidad) {
        MovimientoForm.Linea l = new MovimientoForm.Linea();
        l.setIdProducto(p.getIdProducto());
        l.setCantidadPedida(pedida);
        l.setCantidad(cantidad);
        return l;
    }

    private AjusteForm ajuste(Producto p, int cantidad, String motivo) {
        AjusteForm f = new AjusteForm();
        f.setIdProducto(p.getIdProducto());
        f.setCantidad(cantidad);
        f.setMotivo(motivo);
        return f;
    }

    private int stockDe(Producto p) {
        return productoRepository.findById(p.getIdProducto()).orElseThrow().getStock();
    }
}
