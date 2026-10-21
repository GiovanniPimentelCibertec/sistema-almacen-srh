package pe.srh.almacen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Cabecera de una ENTRADA, SALIDA o AJUSTE. */
@Entity
@Table(name = "movimiento")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "id_sede")
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(length = 200)
    private String observacion;

    // ACTIVO o ANULADO: los movimientos no se borran, se anulan
    @Column(nullable = false, length = 10)
    private String estado = "ACTIVO";

    // Solo salidas a hospitales: null = pendiente de confirmar, CONFORME o DIFERENTE
    @Column(name = "confirmado_sede", length = 10)
    private String confirmadoSede;

    @OneToMany(mappedBy = "movimiento", cascade = CascadeType.ALL)
    private List<MovimientoDetalle> detalles = new ArrayList<>();

    /** Una salida es incompleta si en algún producto se despachó menos de lo pedido. */
    @Transient
    public boolean isEntregaIncompleta() {
        return detalles.stream().anyMatch(d -> d.getCantidadPedida() != null
                && d.getCantidad() < d.getCantidadPedida());
    }

    public Integer getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(Integer idMovimiento) { this.idMovimiento = idMovimiento; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Sede getSede() { return sede; }
    public void setSede(Sede sede) { this.sede = sede; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getConfirmadoSede() { return confirmadoSede; }
    public void setConfirmadoSede(String confirmadoSede) { this.confirmadoSede = confirmadoSede; }

    public List<MovimientoDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<MovimientoDetalle> detalles) { this.detalles = detalles; }
}
