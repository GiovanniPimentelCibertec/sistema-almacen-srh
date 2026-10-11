package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import pe.srh.almacen.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findAllByOrderByNombreAsc();
}
