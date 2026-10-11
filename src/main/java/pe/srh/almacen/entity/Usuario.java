package pe.srh.almacen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/** Persona que usa el sistema. Rol: ADMINISTRADOR, JEFE o ASISTENTE. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @NotBlank(message = "Ingrese los nombres")
    @Column(nullable = false, length = 100)
    private String nombres;

    @NotBlank(message = "Ingrese el usuario")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @NotBlank(message = "Seleccione el rol")
    @Column(nullable = false, length = 20)
    private String rol;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
