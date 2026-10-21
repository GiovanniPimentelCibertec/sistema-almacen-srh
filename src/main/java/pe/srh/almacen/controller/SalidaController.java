package pe.srh.almacen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import pe.srh.almacen.dto.MovimientoForm;
import pe.srh.almacen.entity.Movimiento;

/** Salidas de almacén hacia la planta o las sedes hospitalarias (caso transaccional, CU-06). */
@Controller
@RequestMapping("/salidas")
public class SalidaController extends MovimientoControllerBase {

    @Override
    protected String tipo() { return "SALIDA"; }

    @Override
    protected String ruta() { return "/salidas"; }

    @Override
    protected Movimiento registrar(MovimientoForm form, String username) {
        return movimientoService.registrarSalida(form, username);
    }
}
