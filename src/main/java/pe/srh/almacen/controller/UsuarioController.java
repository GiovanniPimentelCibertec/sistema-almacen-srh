package pe.srh.almacen.controller;

import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.srh.almacen.entity.Usuario;
import pe.srh.almacen.repository.SedeRepository;
import pe.srh.almacen.repository.UsuarioRepository;

/** Mantenimiento de usuarios (solo ADMINISTRADOR). */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final Set<String> ROLES = Set.of("ADMINISTRADOR", "JEFE", "ASISTENTE", "SEDE");

    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, SedeRepository sedeRepository,
                             PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.sedeRepository = sedeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return mostrarFormulario(new Usuario(), model);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        return usuario == null ? "redirect:/usuarios" : mostrarFormulario(usuario, model);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("usuario") Usuario usuario, BindingResult result,
                          @RequestParam(required = false) String clave, Model model, RedirectAttributes flash) {
        boolean esNuevo = usuario.getIdUsuario() == null;
        // El usuario (login) no se puede repetir
        boolean repetido = esNuevo ? usuarioRepository.existsByUsername(usuario.getUsername())
                : usuarioRepository.existsByUsernameAndIdUsuarioNot(usuario.getUsername(), usuario.getIdUsuario());
        if (repetido) {
            result.rejectValue("username", "repetido", "Ese usuario ya existe");
        }
        // Al crear la clave es obligatoria; al editar es opcional, pero si se escribe debe tener 6 o más
        if ((esNuevo && clave == null) || (clave != null && clave.length() < 6)) {
            result.rejectValue("password", "clave", "La contraseña debe tener al menos 6 caracteres");
        }
        if (usuario.getRol() != null && !usuario.getRol().isBlank() && !ROLES.contains(usuario.getRol())) {
            result.rejectValue("rol", "rol", "Seleccione un rol válido");
        }
        // Un encargado de sede necesita un hospital activo; los demás roles no tienen sede
        if ("SEDE".equals(usuario.getRol())) {
            if (!result.hasFieldErrors("sede") && (usuario.getSede() == null || !"HOSPITAL".equals(usuario.getSede().getTipo())
                    || !usuario.getSede().getActivo())) {
                result.rejectValue("sede", "sede", "Elija el hospital del encargado");
            }
        } else {
            usuario.setSede(null);
        }
        if (!esNuevo) {
            Usuario actual = usuarioRepository.findById(usuario.getIdUsuario()).orElse(null);
            if (actual == null) {
                return "redirect:/usuarios";
            }
            // El estado se cambia con el botón Activar/Desactivar, no desde este formulario
            usuario.setActivo(actual.getActivo());
            if (esUltimoAdministrador(actual) && !"ADMINISTRADOR".equals(usuario.getRol())) {
                result.rejectValue("rol", "rol", "Debe quedar al menos un administrador activo");
            }
        }
        if (result.hasErrors()) {
            return mostrarFormulario(usuario, model);
        }
        if (clave != null) {
            usuario.setPassword(passwordEncoder.encode(clave));      // se guarda cifrada con BCrypt
        } else {
            usuario.setPassword(usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow().getPassword());
        }
        usuarioRepository.save(usuario);
        flash.addFlashAttribute("mensaje", "Usuario guardado correctamente");
        return "redirect:/usuarios";
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id, Authentication auth, RedirectAttributes flash) {
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            return "redirect:/usuarios";
        }
        if (u.getActivo() && u.getUsername().equals(auth.getName())) {
            flash.addFlashAttribute("error", "No puede desactivar su propio usuario.");
            return "redirect:/usuarios";
        }
        if (esUltimoAdministrador(u)) {
            flash.addFlashAttribute("error", "No se puede desactivar al último administrador activo.");
            return "redirect:/usuarios";
        }
        u.setActivo(!u.getActivo());
        usuarioRepository.save(u);
        flash.addFlashAttribute("mensaje", "Usuario " + (u.getActivo() ? "activado" : "desactivado"));
        return "redirect:/usuarios";
    }

    private boolean esUltimoAdministrador(Usuario u) {
        return "ADMINISTRADOR".equals(u.getRol()) && u.getActivo()
                && usuarioRepository.countByRolAndActivoTrue("ADMINISTRADOR") <= 1;
    }

    private String mostrarFormulario(Usuario usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("hospitales", sedeRepository.findByTipoAndActivoTrueOrderByNombreAsc("HOSPITAL"));
        return "usuarios/form";
    }
}
