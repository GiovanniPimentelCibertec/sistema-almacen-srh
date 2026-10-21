package pe.srh.almacen.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.srh.almacen.dto.MovimientoForm;
import pe.srh.almacen.entity.Movimiento;
import pe.srh.almacen.repository.ProductoRepository;
import pe.srh.almacen.repository.SedeRepository;
import pe.srh.almacen.service.MovimientoException;
import pe.srh.almacen.service.MovimientoService;

/**
 * Pantallas comunes de entradas y salidas: lista, formulario con varios productos y detalle.
 * Cada subclase indica el tipo de movimiento y cómo se registra.
 */
public abstract class MovimientoControllerBase {

    @Autowired
    protected MovimientoService movimientoService;

    @Autowired
    private SedeRepository sedeRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /** ENTRADA o SALIDA. */
    protected abstract String tipo();

    /** Ruta base, por ejemplo "/salidas". */
    protected abstract String ruta();

    protected abstract Movimiento registrar(MovimientoForm form, String username);

    @GetMapping({"", "/"})
    public String listar(Model model) {
        model.addAttribute("movimientos", movimientoService.listar(tipo()));
        return vista("movimientos/listar", model);
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        MovimientoForm form = new MovimientoForm();
        form.getLineas().add(new MovimientoForm.Linea());
        return mostrarFormulario(form, model);
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("form") MovimientoForm form, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            // Por ejemplo, una cantidad con letras o demasiado grande
            model.addAttribute("errores", List.of("Revise las cantidades: deben ser números enteros válidos."));
            return mostrarFormulario(form, model);
        }
        Movimiento m;
        try {
            m = registrar(form, auth.getName());
        } catch (MovimientoException e) {
            // La transacción ya se revirtió: se vuelve al formulario con lo que el usuario escribió
            model.addAttribute("errores", e.getErrores());
            if (form.getLineas().isEmpty()) {
                form.getLineas().add(new MovimientoForm.Linea());
            }
            return mostrarFormulario(form, model);
        }
        String nombre = esSalida() ? "Salida" : "Entrada";
        String mensaje = nombre + " N.° " + m.getIdMovimiento() + " registrada correctamente";
        if (m.isEntregaIncompleta()) {
            mensaje += " (entrega incompleta: se despachó menos de lo pedido)";
        }
        flash.addFlashAttribute("mensaje", mensaje);
        List<String> porReponer = movimientoService.productosPorReponer(m).stream()
                .map(p -> p.getCodigo() + " " + p.getNombre() + ": quedan " + p.getStock() + " " + p.getUnidad()
                        + " (mínimo " + p.getStockMinimo() + ")")
                .toList();
        if (!porReponer.isEmpty()) {
            flash.addFlashAttribute("porReponer", porReponer);
        }
        return "redirect:" + ruta() + "/" + m.getIdMovimiento();
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable("id") Integer id, Model model) {
        Movimiento m = movimientoService.buscarConDetalle(id);
        if (m == null || !tipo().equals(m.getTipo())) {
            return "redirect:" + ruta();
        }
        model.addAttribute("mov", m);
        return vista("movimientos/detalle", model);
    }

    private String mostrarFormulario(MovimientoForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("sedes", sedeRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("productos", productoRepository.findByActivoTrueOrderByCodigoAsc());
        return vista("movimientos/formulario", model);
    }

    private boolean esSalida() {
        return "SALIDA".equals(tipo());
    }

    // Datos que usan las tres pantallas para mostrar textos de entrada o de salida
    private String vista(String plantilla, Model model) {
        model.addAttribute("esSalida", esSalida());
        model.addAttribute("ruta", ruta());
        return plantilla;
    }
}
