package pe.srh.almacen.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.srh.almacen.entity.Marca;
import pe.srh.almacen.repository.MarcaRepository;

/** Mantenimiento de marcas (rol JEFE). */
@Controller
@RequestMapping("/marcas")
public class MarcaController {

    private final MarcaRepository repository;

    public MarcaController(MarcaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", repository.findAllByOrderByNombreAsc());
        return "marcas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("marca", new Marca());
        return "marcas/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("marca", repository.findById(id).orElseThrow());
        return "marcas/form";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("marca") Marca marca, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "marcas/form";
        }
        try {
            repository.save(marca);
        } catch (DataIntegrityViolationException e) {
            result.rejectValue("nombre", "repetido", "Ese nombre ya está registrado");
            return "marcas/form";
        }
        flash.addFlashAttribute("mensaje", "Marca guardada correctamente");
        return "redirect:/marcas";
    }
}
