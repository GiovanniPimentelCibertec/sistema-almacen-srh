package pe.srh.almacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pe.srh.almacen.entity.Marca;
import pe.srh.almacen.repository.MarcaRepository;

@Controller
@RequestMapping("/marcas")
public class MarcaController {

    @Autowired
    private MarcaRepository marcaRepository;
    
    @GetMapping({"", "/"})
    public String inicio() {
        return "redirect:/marcas/listar";
    }

    @GetMapping("/listar")
    public String listarMarcas(Model model) {
        model.addAttribute("marcas", marcaRepository.findAll());
        return "marcas/listar";
    }

    @GetMapping("/nuevo")
    public String nuevaMarca(Model model) {
        model.addAttribute("marca", new Marca());
        return "marcas/formulario";
    }

    @PostMapping("/guardar")
    public String guardarMarca(@Valid @ModelAttribute("marca") Marca marca, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "marcas/formulario";
        }
        marcaRepository.save(marca);
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

    @GetMapping("/eliminar/{id}")
    public String eliminarMarca(@PathVariable("id") Integer id) {
        try {
            marcaRepository.deleteById(id);
        } catch (Exception e) {
            // Manejar si hay restricción por relaciones con productos
        }
        return "redirect:/marcas/listar";
    }
}