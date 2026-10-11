package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import pe.srh.almacen.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);
    boolean existsByUsernameAndIdUsuarioNot(String username, Integer idUsuario);
    boolean existsByUsername(String username);
}
