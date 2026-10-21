package pe.srh.almacen.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;
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
    public String inicio(Model model, Authentication auth, HttpServletRequest request) {
        // El encargado de un hospital trabaja en su propio panel (se le pasan los avisos, que llegan sin valor)
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SEDE"))) {
            if (request.getParameterMap().containsKey("sinPermiso")) return "redirect:/sede?sinPermiso";
            return request.getParameterMap().containsKey("expirado") ? "redirect:/sede?expirado" : "redirect:/sede";
        }
        usuarioRepository.findByUsername(auth.getName())
                .ifPresent(u -> model.addAttribute("usuarioActual", u));
        model.addAttribute("totalProductos", productoRepository.count());
        model.addAttribute("totalSedes", sedeRepository.count());
        model.addAttribute("totalMovimientos", movimientoRepository.count());
        model.addAttribute("porReponer", productoRepository.findPorReponer());
        model.addAttribute("ultimos", movimientoRepository.findTop8ByOrderByFechaDescIdMovimientoDesc());
        return "inicio";
    }
}
