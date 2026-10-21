package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import pe.srh.almacen.entity.ConsumoSede;

public interface ConsumoSedeRepository extends JpaRepository<ConsumoSede, Integer> {

    // Totales activos por producto y tipo (INICIAL, CONSUMO, AJUSTE) de una sede: [idProducto, tipo, suma]
    @Query("SELECT c.producto.idProducto, c.tipo, SUM(c.cantidad) FROM ConsumoSede c "
         + "WHERE c.sede.idSede = :idSede AND c.estado = 'ACTIVO' GROUP BY c.producto.idProducto, c.tipo")
    List<Object[]> sumarPorProductoYTipo(@Param("idSede") Integer idSede);

    // Consumo activo por producto desde una fecha: [idProducto, suma]
    @Query("SELECT c.producto.idProducto, SUM(c.cantidad) FROM ConsumoSede c "
         + "WHERE c.sede.idSede = :idSede AND c.tipo = 'CONSUMO' AND c.estado = 'ACTIVO' AND c.fecha >= :desde "
         + "GROUP BY c.producto.idProducto")
    List<Object[]> sumarConsumoDesde(@Param("idSede") Integer idSede, @Param("desde") LocalDateTime desde);

    List<ConsumoSede> findTop15BySedeIdSedeOrderByFechaDescIdConsumoDesc(Integer idSede);
}
