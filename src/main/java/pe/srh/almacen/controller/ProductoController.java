package pe.srh.almacen.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.srh.almacen.entity.Producto;
import pe.srh.almacen.repository.CategoriaRepository;
import pe.srh.almacen.repository.MarcaRepository;
import pe.srh.almacen.repository.ProductoRepository;

/** Mantenimiento de productos (JEFE; el ASISTENTE solo ve la lista). */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    public ProductoController(ProductoRepository productoRepository, CategoriaRepository categoriaRepository,
                              MarcaRepository marcaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoRepository.findAllByOrderByCodigoAsc());
        return "productos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return mostrarForm(new Producto(), model);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        return mostrarForm(productoRepository.findById(id).orElseThrow(), model);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("producto") Producto producto, BindingResult result,
                          Model model, RedirectAttributes flash) {
        boolean esNuevo = producto.getIdProducto() == null;
        if (esNuevo && productoRepository.existsByCodigo(producto.getCodigo())) {
            result.rejectValue("codigo", "repetido", "Ese código ya existe");
        }
        if (result.hasErrors()) {
            return mostrarForm(producto, model);
        }
        if (esNuevo) {
            // Un producto nuevo empieza sin stock; el stock solo sube con una ENTRADA
            producto.setStock(0);
            producto.setStockMinimo(producto.getMinimoInicial());
        } else {
            // El stock y el mínimo calculado no se editan a mano
            Producto actual = productoRepository.findById(producto.getIdProducto()).orElseThrow();
            producto.setStock(actual.getStock());
            producto.setStockMinimo(actual.getStockMinimo());
        }
        productoRepository.save(producto);
        flash.addFlashAttribute("mensaje", "Producto guardado correctamente");
        return "redirect:/productos";
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id, RedirectAttributes flash) {
        Producto p = productoRepository.findById(id).orElseThrow();
        p.setActivo(!p.getActivo());
        productoRepository.save(p);
        flash.addFlashAttribute("mensaje", "Producto " + (p.getActivo() ? "activado" : "desactivado"));
        return "redirect:/productos";
    }

    private String mostrarForm(Producto producto, Model model) {
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categoriaRepository.findAllByOrderByNombreAsc());
        model.addAttribute("marcas", marcaRepository.findAllByOrderByNombreAsc());
        return "productos/form";
    }
}
