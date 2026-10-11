package pe.srh.almacen.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.srh.almacen.repository.MovimientoRepository;
import pe.srh.almacen.repository.ProductoRepository;
import pe.srh.almacen.repository.SedeRepository;
import pe.srh.almacen.repository.UsuarioRepository;

@Controller
public class HomeController {

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;

    public HomeController(ProductoRepository productoRepository, MovimientoRepository movimientoRepository,
                          SedeRepository sedeRepository, UsuarioRepository usuarioRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.sedeRepository = sedeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Panel de inicio: resumen y productos por reponer. */
    @GetMapping("/")
    public String inicio(Model model, Authentication auth) {
        usuarioRepository.findByUsername(auth.getName())
                .ifPresent(u -> model.addAttribute("usuarioActual", u));
        model.addAttribute("totalProductos", productoRepository.count());
        model.addAttribute("totalSedes", sedeRepository.count());
        model.addAttribute("totalMovimientos", movimientoRepository.count());
        model.addAttribute("porReponer", productoRepository.findPorReponer());
        model.addAttribute("ultimos", movimientoRepository.findTop8ByOrderByFechaDesc());
        return "inicio";
    }
}
