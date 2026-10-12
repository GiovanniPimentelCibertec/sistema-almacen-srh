package pe.srh.almacen.service;

import java.util.List;
import pe.srh.almacen.entity.Sede;

public interface SedeService {
    List<Sede> listarTodas();
    void guardar(Sede sede);
    Sede buscarPorId(Integer id);
    // Las sedes no se borran (tienen movimientos): se activan o desactivan
    void cambiarEstado(Integer id);
}
