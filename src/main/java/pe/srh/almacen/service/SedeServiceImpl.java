package pe.srh.almacen.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.srh.almacen.entity.Sede;
import pe.srh.almacen.repository.SedeRepository;

@Service
public class SedeServiceImpl implements SedeService {

    @Autowired
    private SedeRepository sedeRepository;

    @Override
    public List<Sede> listarTodas() {
        return sedeRepository.findAllByOrderByNombreAsc();
    }

    @Override
    public void guardar(Sede sede) {
        // La base de datos solo acepta PLANTA u HOSPITAL en mayúsculas
        sede.setTipo(sede.getTipo().toUpperCase());
        if (sede.getActivo() == null) {
            sede.setActivo(true);
        }
        // El formulario no trae la fecha de inicio de control: se conserva la que ya tenía
        if (sede.getIdSede() != null) {
            sedeRepository.findById(sede.getIdSede())
                    .ifPresent(actual -> sede.setFechaInicioControl(actual.getFechaInicioControl()));
        }
        // Un hospital nuevo empieza hoy su control de stock en 0; una planta no lleva ese control
        if ("HOSPITAL".equals(sede.getTipo()) && sede.getFechaInicioControl() == null) {
            sede.setFechaInicioControl(LocalDate.now());
        } else if ("PLANTA".equals(sede.getTipo())) {
            sede.setFechaInicioControl(null);
        }
        sedeRepository.save(sede);
    }

    @Override
    public Sede buscarPorId(Integer id) {
        return sedeRepository.findById(id).orElse(null);
    }

    @Override
    public void cambiarEstado(Integer id) {
        Sede sede = sedeRepository.findById(id).orElseThrow();
        sede.setActivo(!sede.getActivo());
        sedeRepository.save(sede);
    }
}
