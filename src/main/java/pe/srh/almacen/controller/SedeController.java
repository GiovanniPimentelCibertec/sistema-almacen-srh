package pe.srh.almacen.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.srh.almacen.entity.Sede;
import pe.srh.almacen.service.SedeService;

@Controller
@RequestMapping("/sedes")
public class SedeController {

    @Autowired
    private SedeService sedeService;

    // 1. Listar todas las sedes
    @GetMapping
    public String listarSedes(Model model) {
        model.addAttribute("sedes", sedeService.listarTodas());
        return "sedes/listar";
    }

    // 2. Mostrar formulario para nueva sede
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("sede", new Sede());
        return "sedes/formulario";
    }

    // 3. Guardar o actualizar sede (valida campos y nombre repetido)
    @PostMapping("/guardar")
    public String guardarSede(@Valid @ModelAttribute("sede") Sede sede, BindingResult result,
                              Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "sedes/formulario";
        }
        try {
            sedeService.guardar(sede);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Ya existe una sede con ese nombre");
            return "sedes/formulario";
        }
        flash.addFlashAttribute("mensaje", "Sede guardada correctamente");
        return "redirect:/sedes";
    }

    // 4. Mostrar formulario para editar sede
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Integer id, Model model) {
        Sede sede = sedeService.buscarPorId(id);
        model.addAttribute("sede", sede);
        return "sedes/formulario";
    }

    // 5. Activar o desactivar sede (no se borra porque tiene movimientos)
    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable("id") Integer id, RedirectAttributes flash) {
        sedeService.cambiarEstado(id);
        flash.addFlashAttribute("mensaje", "Estado de la sede actualizado");
        return "redirect:/sedes";
    }
}
