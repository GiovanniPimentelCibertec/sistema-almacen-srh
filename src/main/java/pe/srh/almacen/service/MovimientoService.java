package pe.srh.almacen.service;

import java.time.LocalDate;
import java.util.List;
import pe.srh.almacen.dto.AjusteForm;
import pe.srh.almacen.dto.MovimientoForm;
import pe.srh.almacen.entity.Movimiento;
import pe.srh.almacen.entity.MovimientoDetalle;
import pe.srh.almacen.entity.Producto;

public interface MovimientoService {

    /** Movimientos de un tipo (ENTRADA, SALIDA o AJUSTE), del más reciente al más antiguo. */
    List<Movimiento> listar(String tipo);

    Movimiento buscarConDetalle(Integer id);

    /**
     * Caso transaccional: guarda la salida con todos sus productos, descuenta el stock
     * y recalcula el stock mínimo. Si algo falla, no se guarda nada.
     */
    Movimiento registrarSalida(MovimientoForm form, String username);

    /** Guarda la entrada con todos sus productos y suma el stock, en una sola transacción. */
    Movimiento registrarEntrada(MovimientoForm form, String username);

    /** Corrige el stock de un producto según el inventario físico (cantidad con signo y motivo). */
    Movimiento registrarAjuste(AjusteForm form, String username);

    /** Productos del movimiento que quedaron en su stock mínimo o por debajo. */
    List<Producto> productosPorReponer(Movimiento movimiento);

    /** Kardex de un producto entre dos fechas (ambas incluidas). */
    List<MovimientoDetalle> kardex(Integer idProducto, LocalDate desde, LocalDate hasta);
}
