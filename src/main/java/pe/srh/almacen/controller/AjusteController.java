package pe.srh.almacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import pe.srh.almacen.dto.AjusteForm;
import pe.srh.almacen.entity.Movimiento;
import pe.srh.almacen.entity.MovimientoDetalle;
import pe.srh.almacen.repository.ProductoRepository;
import pe.srh.almacen.service.MovimientoException;
import pe.srh.almacen.service.MovimientoService;

/** Ajustes de inventario: el jefe corrige el stock según el conteo físico (CU-07). */
@Controller
@RequestMapping("/ajustes")
public class AjusteController {

    @Autowired
    private MovimientoService movimientoService;

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping({"", "/"})
    public String listar(Model model) {
        model.addAttribute("ajustes", movimientoService.listar("AJUSTE"));
        return "ajustes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return mostrarFormulario(new AjusteForm(), model);
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("ajuste") AjusteForm form, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("errores", List.of("La cantidad debe ser un número entero válido."));
            return mostrarFormulario(form, model);
        }
        try {
            Movimiento m = movimientoService.registrarAjuste(form, auth.getName());
            MovimientoDetalle d = m.getDetalles().get(0);
            flash.addFlashAttribute("mensaje", "Ajuste N.° " + m.getIdMovimiento() + " registrado: "
                    + d.getProducto().getCodigo() + " queda con " + d.getStockResultante() + " "
                    + d.getProducto().getUnidad());
        } catch (MovimientoException e) {
            model.addAttribute("errores", e.getErrores());
            return mostrarFormulario(form, model);
        }
        return "redirect:/ajustes";
    }

    private String mostrarFormulario(AjusteForm form, Model model) {
        model.addAttribute("ajuste", form);
        model.addAttribute("productos", productoRepository.findByActivoTrueOrderByCodigoAsc());
        return "ajustes/formulario";
    }
}
