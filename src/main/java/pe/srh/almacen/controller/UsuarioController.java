package pe.srh.almacen.controller;

import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.srh.almacen.entity.Usuario;
import pe.srh.almacen.repository.UsuarioRepository;

/** Mantenimiento de usuarios (solo ADMINISTRADOR). */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuarios/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("usuario", usuarioRepository.findById(id).orElseThrow());
        return "usuarios/form";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("usuario") Usuario usuario, BindingResult result,
                          @RequestParam(required = false) String clave, RedirectAttributes flash) {
        boolean esNuevo = usuario.getIdUsuario() == null;
        // El usuario (login) no se puede repetir
        boolean repetido = esNuevo ? usuarioRepository.existsByUsername(usuario.getUsername())
                : usuarioRepository.existsByUsernameAndIdUsuarioNot(usuario.getUsername(), usuario.getIdUsuario());
        if (repetido) {
            result.rejectValue("username", "repetido", "Ese usuario ya existe");
        }
        if (esNuevo && (clave == null || clave.length() < 6)) {
            result.rejectValue("password", "clave", "La contraseña debe tener al menos 6 caracteres");
        }
        if (result.hasErrors()) {
            return "usuarios/form";
        }
        if (clave != null && !clave.isBlank()) {
            usuario.setPassword(passwordEncoder.encode(clave));      // se guarda cifrada con BCrypt
        } else {
            usuario.setPassword(usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow().getPassword());
        }
        usuarioRepository.save(usuario);
        flash.addFlashAttribute("mensaje", "Usuario guardado correctamente");
        return "redirect:/usuarios";
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id, RedirectAttributes flash) {
        Usuario u = usuarioRepository.findById(id).orElseThrow();
        u.setActivo(!u.getActivo());
        usuarioRepository.save(u);
        flash.addFlashAttribute("mensaje", "Usuario " + (u.getActivo() ? "activado" : "desactivado"));
        return "redirect:/usuarios";
    }
}
