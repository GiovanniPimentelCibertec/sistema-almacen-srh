package pe.srh.almacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.srh.almacen.entity.Producto;
import pe.srh.almacen.repository.CategoriaRepository;
import pe.srh.almacen.repository.MarcaRepository;
import pe.srh.almacen.repository.ProductoRepository;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    // "/productos" lo ven el jefe y el asistente; "/listar" queda para los enlaces del jefe
    @GetMapping({"", "/", "/listar"})
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoRepository.findAllByOrderByCodigoAsc());
        return "productos/listar";
    }

    @GetMapping("/nuevo")
    public String nuevoProducto(Model model) {
        return mostrarFormulario(new Producto(), model);
    }

    @PostMapping("/guardar")
    public String guardarProducto(@Valid @ModelAttribute("producto") Producto producto, BindingResult result,
                                  Model model, RedirectAttributes flash) {
        boolean esNuevo = producto.getIdProducto() == null;
        if (esNuevo && productoRepository.existsByCodigo(producto.getCodigo())) {
            result.rejectValue("codigo", "repetido", "Ese código ya existe");
        }
        if (result.hasErrors()) {
            return mostrarFormulario(producto, model);
        }
        if (esNuevo) {
            // Un producto nuevo empieza sin stock; el stock solo sube con una ENTRADA
            producto.setStock(0);
            producto.setStockMinimo(producto.getMinimoInicial());
            producto.setActivo(true);
        } else {
            // El stock, el mínimo calculado y el estado no se cambian desde este formulario
            Producto actual = productoRepository.findById(producto.getIdProducto()).orElseThrow();
            producto.setStock(actual.getStock());
            producto.setStockMinimo(actual.getStockMinimo());
            producto.setActivo(actual.getActivo());
        }
        productoRepository.save(producto);
        flash.addFlashAttribute("mensaje", "Producto guardado correctamente");
        return "redirect:/productos/listar";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable("id") Integer id, Model model) {
        Producto producto = productoRepository.findById(id).orElse(null);
        if (producto == null) {
            return "redirect:/productos/listar";
        }
        return mostrarFormulario(producto, model);
    }

    // Los productos no se borran (tienen movimientos): se activan o desactivan
    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable("id") Integer id, RedirectAttributes flash) {
        Producto p = productoRepository.findById(id).orElseThrow();
        p.setActivo(!p.getActivo());
        productoRepository.save(p);
        flash.addFlashAttribute("mensaje", "Producto " + (p.getActivo() ? "activado" : "desactivado"));
        return "redirect:/productos/listar";
    }

    private String mostrarFormulario(Producto producto, Model model) {
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categoriaRepository.findAllByOrderByNombreAsc());
        model.addAttribute("marcas", marcaRepository.findAllByOrderByNombreAsc());
        return "productos/formulario";
    }
}
