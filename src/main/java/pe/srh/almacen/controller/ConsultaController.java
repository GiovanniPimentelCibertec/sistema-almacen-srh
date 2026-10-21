package pe.srh.almacen.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.srh.almacen.entity.MovimientoDetalle;
import pe.srh.almacen.entity.Producto;
import pe.srh.almacen.repository.CategoriaRepository;
import pe.srh.almacen.repository.ProductoRepository;
import pe.srh.almacen.service.MovimientoService;

/** Consultas del sistema: kardex por producto (CU-08) y stock actual (CU-09). */
@Controller
public class ConsultaController {

    @Autowired
    private MovimientoService movimientoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/kardex")
    public String kardex(@RequestParam(required = false) Integer producto,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         Model model) {
        // Sin fechas se muestra el año en curso hasta hoy
        LocalDate hoy = LocalDate.now();
        if (desde == null) desde = hoy.withDayOfYear(1);
        if (hasta == null) hasta = hoy;
        model.addAttribute("productos", productoRepository.findAllByOrderByCodigoAsc());
        model.addAttribute("idProducto", producto);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        if (producto != null) {
            if (desde.isAfter(hasta)) {
                model.addAttribute("error", "La fecha inicial no puede ser posterior a la final.");
            } else {
                Producto p = productoRepository.findById(producto).orElse(null);
                List<MovimientoDetalle> filas = p == null ? List.of() : movimientoService.kardex(producto, desde, hasta);
                model.addAttribute("producto", p);
                model.addAttribute("filas", filas);
                if (!filas.isEmpty()) {
                    // Saldo antes del primer movimiento del rango
                    MovimientoDetalle primero = filas.get(0);
                    boolean esSalida = "SALIDA".equals(primero.getMovimiento().getTipo());
                    model.addAttribute("saldoInicial", primero.getStockResultante()
                            + (esSalida ? primero.getCantidad() : -primero.getCantidad()));
                }
                model.addAttribute("totalEntradas", filas.stream().filter(d -> !"SALIDA".equals(d.getMovimiento().getTipo())
                        && d.getCantidad() > 0).mapToInt(MovimientoDetalle::getCantidad).sum());
                model.addAttribute("totalSalidas", filas.stream().filter(d -> "SALIDA".equals(d.getMovimiento().getTipo())
                        || d.getCantidad() < 0).mapToInt(d -> Math.abs(d.getCantidad())).sum());
            }
        }
        return "consultas/kardex";
    }

    @GetMapping("/stock")
    public String stock(@RequestParam(required = false) Integer categoria,
                        @RequestParam(required = false, defaultValue = "TODOS") String situacion,
                        Model model) {
        List<Producto> productos = productoRepository.findByActivoTrueOrderByCodigoAsc().stream()
                .filter(p -> categoria == null || p.getCategoria().getIdCategoria().equals(categoria))
                .filter(p -> switch (situacion) {
                    case "REPONER" -> p.getStock() <= p.getStockMinimo();
                    case "NORMAL" -> p.getStock() > p.getStockMinimo();
                    default -> true;
                })
                .toList();
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categoriaRepository.findAllByOrderByNombreAsc());
        model.addAttribute("idCategoria", categoria);
        model.addAttribute("situacion", situacion);
        model.addAttribute("totalReponer", productos.stream().filter(p -> p.getStock() <= p.getStockMinimo()).count());
        return "consultas/stock";
    }
}
