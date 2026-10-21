package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import pe.srh.almacen.entity.MovimientoDetalle;

public interface MovimientoDetalleRepository extends JpaRepository<MovimientoDetalle, Integer> {

    // Total despachado de un producto desde una fecha (solo salidas activas); sirve para el stock mínimo
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM MovimientoDetalle d "
         + "WHERE d.producto.idProducto = :idProducto AND d.movimiento.tipo = 'SALIDA' "
         + "AND d.movimiento.estado = 'ACTIVO' AND d.movimiento.fecha >= :desde")
    long sumarSalidasDesde(@Param("idProducto") Integer idProducto, @Param("desde") LocalDateTime desde);

    // Fecha de la primera salida activa de un producto (null si nunca salió); define cuántos días de historia hay
    @Query("SELECT MIN(d.movimiento.fecha) FROM MovimientoDetalle d "
         + "WHERE d.producto.idProducto = :idProducto AND d.movimiento.tipo = 'SALIDA' "
         + "AND d.movimiento.estado = 'ACTIVO'")
    LocalDateTime primeraSalida(@Param("idProducto") Integer idProducto);

    // Kardex: movimientos activos de un producto en un rango de fechas, en orden cronológico
    @Query("SELECT d FROM MovimientoDetalle d JOIN FETCH d.movimiento m LEFT JOIN FETCH m.sede JOIN FETCH m.usuario "
         + "WHERE d.producto.idProducto = :idProducto AND m.estado = 'ACTIVO' "
         + "AND m.fecha >= :desde AND m.fecha < :hasta ORDER BY m.fecha, m.idMovimiento, d.idDetalle")
    List<MovimientoDetalle> findKardex(@Param("idProducto") Integer idProducto,
                                       @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // Lo que un hospital recibió: salidas activas ya confirmadas desde que empezó su control. [idProducto, suma]
    @Query("SELECT d.producto.idProducto, SUM(d.cantidad) FROM MovimientoDetalle d "
         + "WHERE d.movimiento.tipo = 'SALIDA' AND d.movimiento.estado = 'ACTIVO' "
         + "AND d.movimiento.sede.idSede = :idSede AND d.movimiento.confirmadoSede IS NOT NULL "
         + "AND d.movimiento.fecha >= :desde GROUP BY d.producto.idProducto")
    List<Object[]> sumarRecibidoPorSede(@Param("idSede") Integer idSede, @Param("desde") LocalDateTime desde);

    // Reporte de consumo: salidas activas de un periodo con su sede y producto
    @Query("SELECT d FROM MovimientoDetalle d JOIN FETCH d.movimiento m JOIN FETCH m.sede JOIN FETCH d.producto "
         + "WHERE m.tipo = 'SALIDA' AND m.estado = 'ACTIVO' AND m.fecha >= :desde AND m.fecha < :hasta")
    List<MovimientoDetalle> findSalidasDelPeriodo(@Param("desde") LocalDateTime desde,
                                                  @Param("hasta") LocalDateTime hasta);
}
