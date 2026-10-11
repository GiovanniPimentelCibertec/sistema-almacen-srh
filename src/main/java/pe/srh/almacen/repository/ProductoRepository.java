package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import pe.srh.almacen.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findAllByOrderByCodigoAsc();

    boolean existsByCodigo(String codigo);

    // Productos activos cuyo stock llegó al mínimo o está por debajo
    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo ORDER BY p.codigo")
    List<Producto> findPorReponer();
}
