package pe.srh.almacen.service;

import java.util.List;

/**
 * Error de negocio al registrar un movimiento (por ejemplo, stock insuficiente).
 * Al ser RuntimeException, @Transactional revierte toda la operación.
 */
public class MovimientoException extends RuntimeException {

    private final List<String> errores;

    public MovimientoException(List<String> errores) {
        super(String.join(" ", errores));
        this.errores = errores;
    }

    public List<String> getErrores() { return errores; }
}
