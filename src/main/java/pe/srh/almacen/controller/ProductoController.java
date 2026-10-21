package pe.srh.almacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public String guardarProducto(@Valid @ModelAttribute("producto") Producto producto, BindingResult result,
                                  Model model, RedirectAttributes flash) {
        boolean esNuevo = producto.getIdProducto() == null;
        // El código se guarda en mayúsculas para que "bol-001" y "BOL-001" cuenten como el mismo
        if (producto.getCodigo() != null) {
            producto.setCodigo(producto.getCodigo().toUpperCase());
        }
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
            productoRepository.save(producto);
        } else {
            // Se bloquea el producto y solo se copian los campos editables: el código, el stock,
            // el mínimo calculado y el estado no cambian desde aquí (los mueven los movimientos)
            Producto actual = productoRepository.findParaActualizar(producto.getIdProducto()).orElse(null);
            if (actual == null) {
                flash.addFlashAttribute("error", "El producto no existe.");
                return "redirect:/productos/listar";
            }
            actual.setNombre(producto.getNombre());
            actual.setUnidad(producto.getUnidad());
            actual.setCategoria(producto.getCategoria());
            actual.setMarca(producto.getMarca());
            actual.setDiasReposicion(producto.getDiasReposicion());
            actual.setMinimoInicial(producto.getMinimoInicial());
        }
        flash.addFlashAttribute("mensaje", "Producto guardado correctamente");
        return "redirect:/productos/listar";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable("id") Integer id, Model model, RedirectAttributes flash) {
        Producto producto = productoRepository.findById(id).orElse(null);
        if (producto == null) {
            flash.addFlashAttribute("error", "El producto no existe.");
            return "redirect:/productos/listar";
        }
        return mostrarFormulario(producto, model);
    }

    // Los productos no se borran (tienen movimientos): se activan o desactivan
    @PostMapping("/estado/{id}")
    @Transactional
    public String cambiarEstado(@PathVariable("id") Integer id, RedirectAttributes flash) {
        Producto p = productoRepository.findParaActualizar(id).orElse(null);
        if (p == null) {
            flash.addFlashAttribute("error", "El producto no existe.");
            return "redirect:/productos/listar";
        }
        p.setActivo(!p.getActivo());
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
