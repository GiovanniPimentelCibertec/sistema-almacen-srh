package pe.srh.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import pe.srh.almacen.entity.Sede;

public interface SedeRepository extends JpaRepository<Sede, Integer> {

    List<Sede> findAllByOrderByNombreAsc();

    List<Sede> findByActivoTrueOrderByNombreAsc();

    List<Sede> findByTipoAndActivoTrueOrderByNombreAsc(String tipo);

    // Bloquea la sede mientras se registra un consumo, para que dos registros a la vez no pasen el stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sede s WHERE s.idSede = :id")
    Optional<Sede> findParaActualizar(@Param("id") Integer id);
}
