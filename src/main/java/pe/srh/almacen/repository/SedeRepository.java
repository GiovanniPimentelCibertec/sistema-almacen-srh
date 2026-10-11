package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import pe.srh.almacen.entity.Sede;

public interface SedeRepository extends JpaRepository<Sede, Integer> {

    List<Sede> findAllByOrderByNombreAsc();
}
