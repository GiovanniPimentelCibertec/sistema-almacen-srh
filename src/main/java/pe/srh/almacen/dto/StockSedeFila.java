package pe.srh.almacen.dto;

import pe.srh.almacen.entity.Producto;

/** Stock de un producto en un hospital: lo disponible, lo que viene en camino y lo consumido. */
public class StockSedeFila {

    private final Producto producto;
    private final int stock;
    private final int enCamino;
    private final int consumo7dias;
    private final int consumo30dias;

    public StockSedeFila(Producto producto, int stock, int enCamino, int consumo7dias, int consumo30dias) {
        this.producto = producto;
        this.stock = stock;
        this.enCamino = enCamino;
        this.consumo7dias = consumo7dias;
        this.consumo30dias = consumo30dias;
    }

    public Producto getProducto() { return producto; }
    public int getStock() { return stock; }
    public int getEnCamino() { return enCamino; }
    public int getConsumo7dias() { return consumo7dias; }
    public int getConsumo30dias() { return consumo30dias; }
}
