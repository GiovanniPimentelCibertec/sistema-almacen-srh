package pe.srh.almacen.service;

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
        sedeRepository.save(sede);
    }

    @Override
    public Sede buscarPorId(Integer id) {
        return sedeRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminar(Integer id) {
        sedeRepository.deleteById(id);
    }
}