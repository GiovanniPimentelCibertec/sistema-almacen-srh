package pe.srh.almacen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Lugar a donde se envían los productos (planta u hospital). */
@Entity
@Table(name = "sede")
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    private Integer idSede;

    @NotBlank(message = "Ingrese el nombre")
    @Size(max = 80, message = "Máximo 80 caracteres")
    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @NotBlank(message = "Seleccione el tipo")
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Boolean activo = true;

    // Día en que el hospital empezó a controlar su stock (en 0) y a registrar su consumo
    @Column(name = "fecha_inicio_control")
    private LocalDate fechaInicioControl;

    /** El hospital ya registra su propio consumo en el sistema. */
    @Transient
    public boolean isControlIniciado() {
        return fechaInicioControl != null;
    }

    public LocalDate getFechaInicioControl() { return fechaInicioControl; }
    public void setFechaInicioControl(LocalDate fechaInicioControl) { this.fechaInicioControl = fechaInicioControl; }

    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
