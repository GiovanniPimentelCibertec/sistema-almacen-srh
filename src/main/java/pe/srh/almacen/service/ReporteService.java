package pe.srh.almacen.service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import pe.srh.almacen.entity.MovimientoDetalle;
import pe.srh.almacen.entity.Producto;
import pe.srh.almacen.repository.MovimientoDetalleRepository;
import pe.srh.almacen.repository.ProductoRepository;

/** Genera los dos reportes PDF con JasperReports (CU-10) a partir de los .jrxml de src/main/resources/reportes. */
@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ES = Locale.forLanguageTag("es-PE");

    // Cada .jrxml se compila una sola vez y queda en memoria
    private final Map<String, JasperReport> compilados = new ConcurrentHashMap<>();

    @Autowired
    private MovimientoDetalleRepository detalleRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Reporte 1: despachos del almacén central a cada sede en un mes, por producto (salidas activas).
     * No es el consumo que registran los hospitales: es lo que el almacén les envió.
     */
    @Transactional(readOnly = true)
    public byte[] consumoPorSede(YearMonth mes, String usuario) {
        LocalDateTime desde = mes.atDay(1).atStartOfDay();
        LocalDateTime hasta = mes.plusMonths(1).atDay(1).atStartOfDay();

        // Agrupar por sede y producto: veces que salió, total pedido y total despachado
        Map<String, Map<String, Object>> filas = new LinkedHashMap<>();
        List<MovimientoDetalle> detalles = detalleRepository.findSalidasDelPeriodo(desde, hasta);
        // Salidas distintas (envíos) de cada sede: una salida puede traer varios productos
        Map<String, Set<Integer>> salidasPorSede = new HashMap<>();
        detalles.forEach(d -> salidasPorSede.computeIfAbsent(d.getMovimiento().getSede().getNombre(),
                k -> new HashSet<>()).add(d.getMovimiento().getIdMovimiento()));
        detalles.stream()
                .sorted(Comparator.comparing((MovimientoDetalle d) -> d.getMovimiento().getSede().getNombre())
                        .thenComparing(d -> d.getProducto().getCodigo()))
                .forEach(d -> {
                    String sede = d.getMovimiento().getSede().getNombre();
                    Producto p = d.getProducto();
                    Map<String, Object> f = filas.computeIfAbsent(sede + "|" + p.getCodigo(), k -> {
                        Map<String, Object> n = new HashMap<>();
                        n.put("sede", sede);
                        n.put("codigo", p.getCodigo());
                        n.put("producto", p.getNombre());
                        n.put("unidad", p.getUnidad());
                        n.put("pedido", 0);
                        n.put("despachado", 0);
                        n.put("salidas", 0);
                        n.put("salidasSede", salidasPorSede.get(sede).size());
                        return n;
                    });
                    int pedido = d.getCantidadPedida() != null ? d.getCantidadPedida() : d.getCantidad();
                    f.put("pedido", (Integer) f.get("pedido") + pedido);
                    f.put("despachado", (Integer) f.get("despachado") + d.getCantidad());
                    f.put("salidas", (Integer) f.get("salidas") + 1);
                });

        Map<String, Object> parametros = new HashMap<>();
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, ES);
        parametros.put("PERIODO", nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1) + " " + mes.getYear());
        parametros.put("GENERADO", LocalDateTime.now().format(FECHA_HORA));
        parametros.put("USUARIO", usuario);
        return generar("consumo_sede", parametros, new ArrayList<>(filas.values()));
    }

    /** Reporte 2: productos activos en su stock mínimo o por debajo, con la cantidad sugerida a comprar. */
    @Transactional(readOnly = true)
    public byte[] productosPorReponer(String usuario) {
        List<Map<String, ?>> filas = new ArrayList<>();
        for (Producto p : productoRepository.findPorReponer()) {
            Map<String, Object> f = new HashMap<>();
            f.put("codigo", p.getCodigo());
            f.put("producto", p.getNombre());
            f.put("categoria", p.getCategoria().getNombre());
            f.put("unidad", p.getUnidad());
            f.put("stock", p.getStock());
            f.put("minimo", p.getStockMinimo());
            // Volver al mínimo y cubrir un ciclo más de reposición
            f.put("sugerido", Math.max(2 * p.getStockMinimo() - p.getStock(), 1));
            filas.add(f);
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("GENERADO", LocalDateTime.now().format(FECHA_HORA));
        parametros.put("USUARIO", usuario);
        return generar("productos_reponer", parametros, filas);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private byte[] generar(String nombre, Map<String, Object> parametros, List<? extends Map<String, ?>> filas) {
        try {
            JasperReport reporte = compilados.computeIfAbsent(nombre, this::compilar);
            JasperPrint impreso = JasperFillManager.fillReport(reporte, parametros,
                    new JRMapCollectionDataSource((List) filas));
            return JasperExportManager.exportReportToPdf(impreso);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el reporte " + nombre, e);
        }
    }

    private JasperReport compilar(String nombre) {
        try (InputStream jrxml = getClass().getResourceAsStream("/reportes/" + nombre + ".jrxml")) {
            return JasperCompileManager.compileReport(jrxml);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo compilar el reporte " + nombre, e);
        }
    }
}
