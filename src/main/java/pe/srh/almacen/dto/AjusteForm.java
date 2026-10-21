package pe.srh.almacen.dto;

/** Datos del formulario de ajuste: un producto, la cantidad a sumar (+) o restar (-) y el motivo. */
public class AjusteForm {

    private Integer idProducto;
    private Integer cantidad;
    private String motivo;

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
