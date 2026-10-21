package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import pe.srh.almacen.entity.Movimiento;

public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    List<Movimiento> findTop8ByOrderByFechaDescIdMovimientoDesc();

    // Movimientos de un tipo (ENTRADA, SALIDA o AJUSTE) con sus productos, del más reciente al más antiguo
    @Query("SELECT DISTINCT m FROM Movimiento m LEFT JOIN FETCH m.detalles d LEFT JOIN FETCH d.producto "
         + "WHERE m.tipo = :tipo ORDER BY m.fecha DESC, m.idMovimiento DESC")
    List<Movimiento> findPorTipoConDetalle(@Param("tipo") String tipo);

    @Query("SELECT DISTINCT m FROM Movimiento m LEFT JOIN FETCH m.detalles d LEFT JOIN FETCH d.producto "
         + "WHERE m.idMovimiento = :id")
    Optional<Movimiento> findConDetalle(@Param("id") Integer id);

    // Envíos a un hospital que aún no confirma, desde que empezó a registrar su consumo
    @Query("SELECT DISTINCT m FROM Movimiento m LEFT JOIN FETCH m.detalles d LEFT JOIN FETCH d.producto "
         + "WHERE m.tipo = 'SALIDA' AND m.estado = 'ACTIVO' AND m.sede.idSede = :idSede "
         + "AND m.confirmadoSede IS NULL AND m.fecha >= :desde ORDER BY m.fecha, m.idMovimiento")
    List<Movimiento> findEnviosPendientes(@Param("idSede") Integer idSede, @Param("desde") LocalDateTime desde);
}
