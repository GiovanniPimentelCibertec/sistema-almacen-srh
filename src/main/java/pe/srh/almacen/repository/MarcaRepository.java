package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import pe.srh.almacen.entity.Marca;

public interface MarcaRepository extends JpaRepository<Marca, Integer> {

    List<Marca> findAllByOrderByNombreAsc();
}
