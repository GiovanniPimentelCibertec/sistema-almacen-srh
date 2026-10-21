package pe.srh.almacen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/** Grupo de productos: bolsas, EPP, uniformes, químicos, útiles. */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @NotBlank(message = "Ingrese el nombre")
    @Size(max = 80, message = "Máximo 80 caracteres")
    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
