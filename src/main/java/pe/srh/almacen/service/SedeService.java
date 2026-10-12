package pe.srh.almacen.service;

import java.util.List;
import pe.srh.almacen.entity.Sede;

public interface SedeService {
    List<Sede> listarTodas();
    void guardar(Sede sede);
    Sede buscarPorId(Integer id);
    void eliminar(Integer id);
}