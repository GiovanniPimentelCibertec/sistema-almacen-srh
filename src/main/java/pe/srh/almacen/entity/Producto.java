package pe.srh.almacen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/** Producto del almacén. El stock solo cambia con los movimientos. */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @NotBlank(message = "Ingrese el código")
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @NotBlank(message = "Ingrese el nombre")
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotBlank(message = "Ingrese la unidad")
    @Column(nullable = false, length = 20)
    private String unidad;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @NotNull(message = "Ingrese los días de reposición")
    @Min(value = 1, message = "Debe ser mayor a 0")
    @Column(name = "dias_reposicion", nullable = false)
    private Integer diasReposicion;

    @NotNull(message = "Ingrese el mínimo inicial")
    @Min(value = 0, message = "No puede ser negativo")
    @Column(name = "minimo_inicial", nullable = false)
    private Integer minimoInicial;

    @NotNull(message = "Seleccione la categoría")
    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @NotNull(message = "Seleccione la marca")
    @ManyToOne
    @JoinColumn(name = "id_marca", nullable = false)
    private Marca marca;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public Integer getDiasReposicion() { return diasReposicion; }
    public void setDiasReposicion(Integer diasReposicion) { this.diasReposicion = diasReposicion; }

    public Integer getMinimoInicial() { return minimoInicial; }
    public void setMinimoInicial(Integer minimoInicial) { this.minimoInicial = minimoInicial; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
