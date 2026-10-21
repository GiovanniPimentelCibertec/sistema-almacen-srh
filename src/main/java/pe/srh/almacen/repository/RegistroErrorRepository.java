package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import pe.srh.almacen.entity.RegistroError;

public interface RegistroErrorRepository extends JpaRepository<RegistroError, Integer> {

    List<RegistroError> findByEstadoOrderByFechaReporteDesc(String estado);

    // Diferencias de un hospital en un estado (las de envío vienen de una salida a esa sede)
    List<RegistroError> findByMovimientoSedeIdSedeAndEstadoOrderByFechaReporteDesc(Integer idSede, String estado);
}
