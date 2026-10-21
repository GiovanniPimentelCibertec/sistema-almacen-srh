package pe.srh.almacen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Error reportado para que el jefe lo revise. Aquí se usa para las diferencias de envío:
 * el hospital indica que de una salida llegó una cantidad distinta a la despachada.
 */
@Entity
@Table(name = "registro_error")
public class RegistroError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_error")
    private Integer idError;

    @ManyToOne
    @JoinColumn(name = "id_consumo")
    private ConsumoSede consumo;

    @ManyToOne
    @JoinColumn(name = "id_movimiento")
    private Movimiento movimiento;

    // CORREGIR, ANULAR o DIFERENCIA
    @Column(nullable = false, length = 10)
    private String accion;

    @Column(name = "valor_anterior")
    private Integer valorAnterior;

    @Column(name = "valor_nuevo")
    private Integer valorNuevo;

    // DIGITACION, DUPLICADO, PRODUCTO, ENVIO u OTRO
    @Column(nullable = false, length = 20)
    private String motivo;

    @Column(length = 200)
    private String detalle;

    @ManyToOne
    @JoinColumn(name = "id_usuario_reporta", nullable = false)
    private Usuario usuarioReporta;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @ManyToOne
    @JoinColumn(name = "id_usuario_revisa")
    private Usuario usuarioRevisa;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    // PENDIENTE, APROBADO o RECHAZADO
    @Column(nullable = false, length = 10)
    private String estado;

    public Integer getIdError() { return idError; }
    public void setIdError(Integer idError) { this.idError = idError; }

    public ConsumoSede getConsumo() { return consumo; }
    public void setConsumo(ConsumoSede consumo) { this.consumo = consumo; }

    public Movimiento getMovimiento() { return movimiento; }
    public void setMovimiento(Movimiento movimiento) { this.movimiento = movimiento; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public Integer getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(Integer valorAnterior) { this.valorAnterior = valorAnterior; }

    public Integer getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(Integer valorNuevo) { this.valorNuevo = valorNuevo; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public Usuario getUsuarioReporta() { return usuarioReporta; }
    public void setUsuarioReporta(Usuario usuarioReporta) { this.usuarioReporta = usuarioReporta; }

    public LocalDateTime getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }

    public Usuario getUsuarioRevisa() { return usuarioRevisa; }
    public void setUsuarioRevisa(Usuario usuarioRevisa) { this.usuarioRevisa = usuarioRevisa; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
