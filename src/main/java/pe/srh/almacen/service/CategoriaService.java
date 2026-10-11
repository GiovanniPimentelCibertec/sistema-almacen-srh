package pe.srh.almacen.service;

import pe.srh.almacen.entity.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaService {
    List<Categoria> listarTodas();
    Optional<Categoria> buscarPorId(Integer id);
    Categoria guardar(Categoria categoria);
    void eliminar(Integer id);
}