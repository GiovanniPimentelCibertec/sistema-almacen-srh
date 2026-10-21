package pe.srh.almacen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de un hospital: CONSUMO (lo usado en un turno, resta) o AJUSTE (corrección con signo,
 * por ejemplo lo que no llegó de un envío). No se borra: se anula.
 */
@Entity
@Table(name = "consumo_sede")
public class ConsumoSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consumo")
    private Integer idConsumo;

    @ManyToOne
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private Integer cantidad;

    // MANANA, TARDE o NOCHE; solo para CONSUMO
    @Column(length = 10)
    private String turno;

    @Column(length = 100)
    private String motivo;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 10)
    private String estado = "ACTIVO";

    /** Turno como se muestra en pantalla: mañana, tarde o noche. */
    public String getTurnoNombre() {
        return nombreTurno(turno);
    }

    public static String nombreTurno(String turno) {
        return switch (turno == null ? "" : turno) {
            case "MANANA" -> "mañana";
            case "TARDE" -> "tarde";
            case "NOCHE" -> "noche";
            default -> "-";
        };
    }

    public Integer getIdConsumo() { return idConsumo; }
    public void setIdConsumo(Integer idConsumo) { this.idConsumo = idConsumo; }

    public Sede getSede() { return sede; }
    public void setSede(Sede sede) { this.sede = sede; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
