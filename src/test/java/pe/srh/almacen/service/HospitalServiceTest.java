package pe.srh.almacen.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pe.srh.almacen.dto.MovimientoForm;
import pe.srh.almacen.entity.*;
import pe.srh.almacen.repository.*;

/** Pruebas del módulo de hospitales: inicio en 0, envíos, consumo por turno y stock por sede. */
@SpringBootTest
class HospitalServiceTest {

    @Autowired private HospitalService hospitalService;
    @Autowired private MovimientoService movimientoService;
    @Autowired private RegistroErrorRepository errorRepository;
    @Autowired private ConsumoSedeRepository consumoRepository;
    @Autowired private MovimientoRepository movimientoRepository;
    @Autowired private MovimientoDetalleRepository detalleRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private SedeRepository sedeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private MarcaRepository marcaRepository;

    private Sede kaelin;
    private Sede barton;
    private Producto bolsa;
    private Producto alcohol;

    @BeforeEach
    void prepararDatos() {
        Categoria cat = new Categoria();
        cat.setNombre("Bolsas");
        categoriaRepository.save(cat);
        Marca marca = new Marca();
        marca.setNombre("Genérica");
        marcaRepository.save(marca);
        kaelin = crearHospital("Hospital Kaelin");
        barton = crearHospital("Hospital Barton");
        crearUsuario("jefe", "JEFE", null);
        crearUsuario("kaelin", "SEDE", kaelin);
        crearUsuario("barton", "SEDE", barton);
        bolsa = crearProducto("BOL-001", "Bolsa roja", 0, cat, marca);
        alcohol = crearProducto("QUI-001", "Alcohol", 0, cat, marca);
        // Igual que al arrancar el sistema: los hospitales empiezan hoy con todo en 0
        assertEquals(2, hospitalService.iniciarHospitalesPendientes());
    }

    @AfterEach
    void limpiar() {
        errorRepository.deleteAll();
        consumoRepository.deleteAll();
        detalleRepository.deleteAll();
        movimientoRepository.deleteAll();
        usuarioRepository.deleteAll();
        productoRepository.deleteAll();
        sedeRepository.deleteAll();
        categoriaRepository.deleteAll();
        marcaRepository.deleteAll();
    }

    /** El ejemplo pedido: entrada de 100 al almacén, salida de 100 a Kaelin y consumo diario en el hospital. */
    @Test
    void flujoCompletoEntradaEnvioYConsumoDiario() {
        movimientoService.registrarEntrada(form(null, linea(bolsa, null, 100)), "jefe");
        assertEquals(100, stockAlmacen(bolsa));

        Movimiento envio = movimientoService.registrarSalida(form(kaelin, linea(bolsa, 100, 100)), "jefe");
        assertEquals(0, stockAlmacen(bolsa), "El almacén central queda sin stock");

        Sede k = recargar(kaelin);
        assertEquals(0, hospitalService.stockDeProducto(k, bolsa.getIdProducto()), "Aún no confirma el envío");
        assertEquals(100, fila(k, bolsa).getEnCamino(), "Los 100 figuran en camino");
        assertEquals(1, hospitalService.enviosPendientes(k).size());

        assertTrue(hospitalService.confirmarEnvio(k.getIdSede(), envio.getIdMovimiento(), null, "kaelin"));
        assertEquals(100, hospitalService.stockDeProducto(k, bolsa.getIdProducto()));
        assertEquals(0, fila(k, bolsa).getEnCamino());
        assertTrue(hospitalService.enviosPendientes(k).isEmpty());

        // Consumo diario
        ConsumoSede c = hospitalService.registrarConsumo(k.getIdSede(), bolsa.getIdProducto(), 12, "kaelin");
        hospitalService.registrarConsumo(k.getIdSede(), bolsa.getIdProducto(), 13, "kaelin");
        assertEquals(HospitalService.turnoPara(LocalTime.now()), c.getTurno());
        assertEquals(75, hospitalService.stockDeProducto(k, bolsa.getIdProducto()));
        assertEquals(25, fila(k, bolsa).getConsumo7dias());
    }

    @Test
    void noSePuedeConsumirMasDeLoQueHay() {
        assertThrows(MovimientoException.class,
                () -> hospitalService.registrarConsumo(kaelin.getIdSede(), bolsa.getIdProducto(), 1, "kaelin"),
                "El hospital empieza en 0: no hay nada que consumir");

        movimientoService.registrarEntrada(form(null, linea(bolsa, null, 10)), "jefe");
        Movimiento envio = movimientoService.registrarSalida(form(kaelin, linea(bolsa, 10, 10)), "jefe");
        hospitalService.confirmarEnvio(kaelin.getIdSede(), envio.getIdMovimiento(), null, "kaelin");
        MovimientoException e = assertThrows(MovimientoException.class,
                () -> hospitalService.registrarConsumo(kaelin.getIdSede(), bolsa.getIdProducto(), 11, "kaelin"));
        assertTrue(e.getMessage().contains("solo hay 10"));
        assertThrows(MovimientoException.class,
                () -> hospitalService.registrarConsumo(kaelin.getIdSede(), bolsa.getIdProducto(), 0, "kaelin"));
        assertEquals(10, hospitalService.stockDeProducto(recargar(kaelin), bolsa.getIdProducto()));
    }

    @Test
    void elHospitalEmpiezaEnCeroYLosEnviosAnterioresNoCuentan() {
        Sede k = recargar(kaelin);
        assertTrue(k.isControlIniciado());
        assertEquals(0, hospitalService.stockDeProducto(k, bolsa.getIdProducto()));
        assertEquals(0, hospitalService.iniciarHospitalesPendientes(), "No se vuelve a iniciar");

        // Un envío de ayer (antes de empezar el control) no queda pendiente ni suma al hospital
        movimientoService.registrarEntrada(form(null, linea(bolsa, null, 20)), "jefe");
        Movimiento antiguo = movimientoService.registrarSalida(form(kaelin, linea(bolsa, 20, 20)), "jefe");
        antiguo.setFecha(java.time.LocalDateTime.now().minusDays(1));
        movimientoRepository.save(antiguo);
        assertTrue(hospitalService.enviosPendientes(k).isEmpty());
        assertEquals(0, hospitalService.stockDeProducto(k, bolsa.getIdProducto()));
        assertThrows(MovimientoException.class,
                () -> hospitalService.confirmarEnvio(k.getIdSede(), antiguo.getIdMovimiento(), null, "kaelin"));
    }

    @Test
    void envioQueLlegaIncompletoSeReportaAlJefe() {
        movimientoService.registrarEntrada(form(null, linea(bolsa, null, 100), linea(alcohol, null, 10)), "jefe");
        Movimiento envio = movimientoService.registrarSalida(
                form(kaelin, linea(bolsa, 50, 50), linea(alcohol, 10, 10)), "jefe");
        Integer detBolsa = envio.getDetalles().get(0).getIdDetalle();
        Integer detAlcohol = envio.getDetalles().get(1).getIdDetalle();

        // No pueden llegar más de las despachadas
        assertThrows(MovimientoException.class, () -> hospitalService.confirmarEnvio(kaelin.getIdSede(),
                envio.getIdMovimiento(), Map.of(detBolsa, 60, detAlcohol, 10), "kaelin"));

        boolean conforme = hospitalService.confirmarEnvio(kaelin.getIdSede(), envio.getIdMovimiento(),
                Map.of(detBolsa, 45, detAlcohol, 10), "kaelin");

        assertFalse(conforme);
        Sede k = recargar(kaelin);
        assertEquals(45, hospitalService.stockDeProducto(k, bolsa.getIdProducto()), "Solo cuenta lo que llegó");
        assertEquals(10, hospitalService.stockDeProducto(k, alcohol.getIdProducto()));
        assertEquals("DIFERENTE", movimientoRepository.findById(envio.getIdMovimiento()).orElseThrow().getConfirmadoSede());
        List<RegistroError> diferencias = hospitalService.diferenciasPendientes();
        assertEquals(1, diferencias.size());
        assertEquals(50, diferencias.get(0).getValorAnterior());
        assertEquals(45, diferencias.get(0).getValorNuevo());
    }

    @Test
    void elJefeRevisaLaDiferenciaYQuedaCerrada() {
        movimientoService.registrarEntrada(form(null, linea(bolsa, null, 100)), "jefe");
        Movimiento envio = movimientoService.registrarSalida(form(kaelin, linea(bolsa, 50, 50)), "jefe");
        Integer det = envio.getDetalles().get(0).getIdDetalle();
        hospitalService.confirmarEnvio(kaelin.getIdSede(), envio.getIdMovimiento(), Map.of(det, 45), "kaelin");
        RegistroError pendiente = hospitalService.diferenciasPendientes(recargar(kaelin)).get(0);

        // Solo APROBADO o RECHAZADO
        assertThrows(MovimientoException.class,
                () -> hospitalService.revisarDiferencia(pendiente.getIdError(), "PENDIENTE", "jefe"));

        RegistroError revisada = hospitalService.revisarDiferencia(pendiente.getIdError(), "APROBADO", "jefe");

        RegistroError guardada = errorRepository.findById(revisada.getIdError()).orElseThrow();
        assertEquals("APROBADO", guardada.getEstado());
        assertEquals("jefe", guardada.getUsuarioRevisa().getUsername(), "Guarda quién revisó");
        assertNotNull(guardada.getFechaRevision(), "Guarda cuándo se revisó");
        assertTrue(hospitalService.diferenciasPendientes().isEmpty(), "Ya no figura como pendiente");
        assertEquals(45, hospitalService.stockDeProducto(recargar(kaelin), bolsa.getIdProducto()),
                "La revisión no cambia el stock del hospital");
        assertThrows(MovimientoException.class,
                () -> hospitalService.revisarDiferencia(pendiente.getIdError(), "RECHAZADO", "jefe"),
                "No se revisa dos veces");
    }

    @Test
    void cadaHospitalVeSoloSusDiferencias() {
        movimientoService.registrarEntrada(form(null, linea(bolsa, null, 100)), "jefe");
        Movimiento aKaelin = movimientoService.registrarSalida(form(kaelin, linea(bolsa, 30, 30)), "jefe");
        Movimiento aBarton = movimientoService.registrarSalida(form(barton, linea(bolsa, 20, 20)), "jefe");
        hospitalService.confirmarEnvio(kaelin.getIdSede(), aKaelin.getIdMovimiento(),
                Map.of(aKaelin.getDetalles().get(0).getIdDetalle(), 25), "kaelin");
        hospitalService.confirmarEnvio(barton.getIdSede(), aBarton.getIdMovimiento(),
                Map.of(aBarton.getDetalles().get(0).getIdDetalle(), 18), "barton");

        List<RegistroError> deKaelin = hospitalService.diferenciasPendientes(recargar(kaelin));
        List<RegistroError> deBarton = hospitalService.diferenciasPendientes(recargar(barton));
        assertEquals(1, deKaelin.size());
        assertEquals(aKaelin.getIdMovimiento(), deKaelin.get(0).getMovimiento().getIdMovimiento());
        assertEquals(1, deBarton.size());
        assertEquals(aBarton.getIdMovimiento(), deBarton.get(0).getMovimiento().getIdMovimiento());

        // Al rechazar la de Barton, la de Kaelin sigue pendiente
        RegistroError r = hospitalService.revisarDiferencia(deBarton.get(0).getIdError(), "RECHAZADO", "jefe");
        assertEquals("RECHAZADO", r.getEstado());
        assertTrue(hospitalService.diferenciasPendientes(recargar(barton)).isEmpty());
        assertEquals(1, hospitalService.diferenciasPendientes(recargar(kaelin)).size());
    }

    @Test
    void cadaHospitalSoloConfirmaSusEnviosYVeSuStock() {
        movimientoService.registrarEntrada(form(null, linea(bolsa, null, 100)), "jefe");
        Movimiento envioKaelin = movimientoService.registrarSalida(form(kaelin, linea(bolsa, 30, 30)), "jefe");

        assertThrows(MovimientoException.class, () -> hospitalService.confirmarEnvio(barton.getIdSede(),
                envioKaelin.getIdMovimiento(), null, "barton"), "Barton no confirma envíos de Kaelin");
        assertTrue(hospitalService.enviosPendientes(recargar(barton)).isEmpty());

        hospitalService.confirmarEnvio(kaelin.getIdSede(), envioKaelin.getIdMovimiento(), null, "kaelin");
        assertThrows(MovimientoException.class, () -> hospitalService.confirmarEnvio(kaelin.getIdSede(),
                envioKaelin.getIdMovimiento(), null, "kaelin"), "No se confirma dos veces");
        assertEquals(30, hospitalService.stockDeProducto(recargar(kaelin), bolsa.getIdProducto()));
        assertEquals(0, hospitalService.stockDeProducto(recargar(barton), bolsa.getIdProducto()));
    }

    @Test
    void elTurnoSeTomaDeLaHora() {
        assertEquals("MANANA", HospitalService.turnoPara(LocalTime.of(6, 0)));
        assertEquals("MANANA", HospitalService.turnoPara(LocalTime.of(13, 59)));
        assertEquals("TARDE", HospitalService.turnoPara(LocalTime.of(14, 0)));
        assertEquals("NOCHE", HospitalService.turnoPara(LocalTime.of(22, 0)));
        assertEquals("NOCHE", HospitalService.turnoPara(LocalTime.of(3, 30)));
    }

    // ---------- Ayudantes ----------

    private Sede crearHospital(String nombre) {
        Sede s = new Sede();
        s.setNombre(nombre);
        s.setTipo("HOSPITAL");
        return sedeRepository.save(s);
    }

    private void crearUsuario(String username, String rol, Sede sede) {
        Usuario u = new Usuario();
        u.setNombres(username);
        u.setUsername(username);
        u.setPassword("x");
        u.setRol(rol);
        u.setSede(sede);
        usuarioRepository.save(u);
    }

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

    private MovimientoForm form(Sede sede, MovimientoForm.Linea... lineas) {
        MovimientoForm f = new MovimientoForm();
        f.setIdSede(sede == null ? null : sede.getIdSede());
        f.getLineas().addAll(List.of(lineas));
        return f;
    }

    private MovimientoForm.Linea linea(Producto p, Integer pedida, int cantidad) {
        MovimientoForm.Linea l = new MovimientoForm.Linea();
        l.setIdProducto(p.getIdProducto());
        l.setCantidadPedida(pedida);
        l.setCantidad(cantidad);
        return l;
    }

    private Sede recargar(Sede s) {
        return sedeRepository.findById(s.getIdSede()).orElseThrow();
    }

    private pe.srh.almacen.dto.StockSedeFila fila(Sede s, Producto p) {
        return hospitalService.stockDeSede(s).stream()
                .filter(f -> f.getProducto().getIdProducto().equals(p.getIdProducto())).findFirst().orElseThrow();
    }

    private int stockAlmacen(Producto p) {
        return productoRepository.findById(p.getIdProducto()).orElseThrow().getStock();
    }
}
