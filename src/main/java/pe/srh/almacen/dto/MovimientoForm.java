package pe.srh.almacen.dto;

import java.util.ArrayList;
import java.util.List;

/** Datos del formulario de una entrada o salida: la sede (solo salidas) y un renglón por producto. */
public class MovimientoForm {

    private Integer idSede;
    private String observacion;
    private List<Linea> lineas = new ArrayList<>();

    /** Un producto del movimiento. En las salidas se indica lo pedido y lo despachado. */
    public static class Linea {
        private Integer idProducto;
        private Integer cantidadPedida;
        private Integer cantidad;

        public Integer getIdProducto() { return idProducto; }
        public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

        public Integer getCantidadPedida() { return cantidadPedida; }
        public void setCantidadPedida(Integer cantidadPedida) { this.cantidadPedida = cantidadPedida; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

        /** Renglón que el usuario agregó pero dejó en blanco. */
        public boolean isVacia() {
            return idProducto == null && cantidadPedida == null && cantidad == null;
        }
    }

    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public List<Linea> getLineas() { return lineas; }
    public void setLineas(List<Linea> lineas) { this.lineas = lineas; }
}
