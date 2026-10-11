package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import pe.srh.almacen.entity.Movimiento;

public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    List<Movimiento> findTop8ByOrderByFechaDesc();
}
