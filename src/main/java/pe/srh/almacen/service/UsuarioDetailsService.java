package pe.srh.almacen.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.srh.almacen.entity.Usuario;
import pe.srh.almacen.repository.UsuarioRepository;

/**
 * Spring Security usa esta clase para buscar al usuario en la base de datos al iniciar sesión.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRol())           // ADMINISTRADOR, JEFE o ASISTENTE
                .disabled(!u.getActivo())    // un usuario desactivado no puede entrar
                .build();
    }
}
