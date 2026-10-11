package pe.srh.almacen.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.srh.almacen.entity.Categoria;
import pe.srh.almacen.service.CategoriaService;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public String listarCategorias(Model model) {	
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "categorias/listar";
    }

    @GetMapping("/nuevo")
    public String nuevaCategoriaForm(Model model) {
        model.addAttribute("categoria", new Categoria());
        return "categorias/formulario";
    }

    @PostMapping("/guardar")
    public String guardarCategoria(@ModelAttribute Categoria categoria, RedirectAttributes redirectAttributes) {
        categoriaService.guardar(categoria);
        redirectAttributes.addFlashAttribute("mensaje", "¡Categoría guardada con éxito!");
        return "redirect:/categorias";
    }

    @GetMapping("/editar/{id}")
    public String editarCategoriaForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Categoria categoria = categoriaService.buscarPorId(id).orElse(null);
        if (categoria == null) {
            redirectAttributes.addFlashAttribute("mensaje", "La categoría no existe.");
            return "redirect:/categorias";
        }
        model.addAttribute("categoria", categoria);
        return "categorias/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCategoria(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "¡Categoría eliminada con éxito!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "No se puede eliminar la categoría porque está asociada a productos.");
        }
        return "redirect:/categorias";
    }
}