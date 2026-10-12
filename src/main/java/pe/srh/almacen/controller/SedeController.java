package pe.srh.almacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    // 3. Guardar o actualizar sede
    @PostMapping("/guardar")
    public String guardarSede(@ModelAttribute("sede") Sede sede) {
        sedeService.guardar(sede);
        return "redirect:/sedes";
    }

    // 4. Mostrar formulario para editar sede
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Integer id, Model model) {
        Sede sede = sedeService.buscarPorId(id);
        model.addAttribute("sede", sede);
        return "sedes/formulario";
    }

    // 5. Eliminar o desactivar sede
    @GetMapping("/eliminar/{id}")
    public String eliminarSede(@PathVariable("id") Integer id) {
        sedeService.eliminar(id);
        return "redirect:/sedes";
    }
}