package pe.srh.almacen.controller;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindingResult;
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
    public String guardarCategoria(@Valid @ModelAttribute Categoria categoria, BindingResult result,
                                   Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "categorias/formulario";
        }
        if (categoria.getIdCategoria() != null && categoriaService.buscarPorId(categoria.getIdCategoria()).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La categoría no existe.");
            return "redirect:/categorias";
        }
        try {
            categoriaService.guardar(categoria);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Ya existe una categoría con ese nombre");
            return "categorias/formulario";
        }
        redirectAttributes.addFlashAttribute("mensaje", "¡Categoría guardada con éxito!");
        return "redirect:/categorias";
    }

    @GetMapping("/editar/{id}")
    public String editarCategoriaForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Categoria categoria = categoriaService.buscarPorId(id).orElse(null);
        if (categoria == null) {
            redirectAttributes.addFlashAttribute("error", "La categoría no existe.");
            return "redirect:/categorias";
        }
        model.addAttribute("categoria", categoria);
        return "categorias/formulario";
    }

    // Por POST para que lo proteja el token CSRF; la base de datos no deja borrar una categoría con productos
    @PostMapping("/eliminar/{id}")
    public String eliminarCategoria(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        if (categoriaService.buscarPorId(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La categoría no existe.");
            return "redirect:/categorias";
        }
        try {
            categoriaService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "¡Categoría eliminada con éxito!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar la categoría porque está asociada a productos.");
        }
        return "redirect:/categorias";
    }
}