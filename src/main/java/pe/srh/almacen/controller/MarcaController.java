package pe.srh.almacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.srh.almacen.entity.Marca;
import pe.srh.almacen.repository.MarcaRepository;

@Controller
@RequestMapping("/marcas")
public class MarcaController {

    @Autowired
    private MarcaRepository marcaRepository;

    @GetMapping({"", "/", "/listar"})
    public String listarMarcas(Model model) {
        model.addAttribute("marcas", marcaRepository.findAllByOrderByNombreAsc());
        return "marcas/listar";
    }

    @GetMapping("/nuevo")
    public String nuevaMarca(Model model) {
        model.addAttribute("marca", new Marca());
        return "marcas/formulario";
    }

    @PostMapping("/guardar")
    public String guardarMarca(@Valid @ModelAttribute("marca") Marca marca, BindingResult result,
                               Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "marcas/formulario";
        }
        try {
            marcaRepository.save(marca);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Ya existe una marca con ese nombre");
            return "marcas/formulario";
        }
        flash.addFlashAttribute("mensaje", "Marca guardada correctamente");
        return "redirect:/marcas/listar";
    }

    @GetMapping("/editar/{id}")
    public String editarMarca(@PathVariable("id") Integer id, Model model) {
        Marca marca = marcaRepository.findById(id).orElse(null);
        if (marca == null) {
            return "redirect:/marcas/listar";
        }
        model.addAttribute("marca", marca);
        return "marcas/formulario";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarMarca(@PathVariable("id") Integer id, RedirectAttributes flash) {
        try {
            marcaRepository.deleteById(id);
            flash.addFlashAttribute("mensaje", "Marca eliminada");
        } catch (DataIntegrityViolationException e) {
            // Si algún producto usa la marca, la base de datos no deja borrarla
            flash.addFlashAttribute("error", "No se puede eliminar la marca porque está asociada a productos.");
        }
        return "redirect:/marcas/listar";
    }
}
