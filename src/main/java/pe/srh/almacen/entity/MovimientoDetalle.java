package pe.srh.almacen.entity;

import jakarta.persistence.*;

/** Cada producto de un movimiento, con la cantidad y el stock que quedó. */
@Entity
@Table(name = "movimiento_detalle")
public class MovimientoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne
    @JoinColumn(name = "id_movimiento", nullable = false)
    private Movimiento movimiento;

    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_pedida")
    private Integer cantidadPedida;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }

    public Movimiento getMovimiento() { return movimiento; }
    public void setMovimiento(Movimiento movimiento) { this.movimiento = movimiento; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidadPedida() { return cantidadPedida; }
    public void setCantidadPedida(Integer cantidadPedida) { this.cantidadPedida = cantidadPedida; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Integer getStockResultante() { return stockResultante; }
    public void setStockResultante(Integer stockResultante) { this.stockResultante = stockResultante; }
}
