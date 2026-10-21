package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.srh.almacen.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findAllByOrderByCodigoAsc();

    boolean existsByCodigo(String codigo);

    List<Producto> findByActivoTrueOrderByCodigoAsc();

    // Bloquea la fila hasta que termine la transacción, para que dos movimientos a la vez no descuadren el stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :id")
    Optional<Producto> findParaActualizar(@Param("id") Integer id);

    // Productos activos cuyo stock llegó al mínimo o está por debajo
    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo ORDER BY p.codigo")
    List<Producto> findPorReponer();
}
