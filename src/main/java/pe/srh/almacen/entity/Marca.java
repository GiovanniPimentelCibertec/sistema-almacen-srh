package pe.srh.almacen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/** Marca del producto. */
@Entity
@Table(name = "marca")
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marca")
    private Integer idMarca;

    @NotBlank(message = "Ingrese el nombre")
    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    public Integer getIdMarca() { return idMarca; }
    public void setIdMarca(Integer idMarca) { this.idMarca = idMarca; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
