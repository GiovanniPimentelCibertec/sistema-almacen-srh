package pe.srh.almacen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import pe.srh.almacen.dto.MovimientoForm;
import pe.srh.almacen.entity.Movimiento;

/** Entradas de mercadería al almacén central (CU-05). */
@Controller
@RequestMapping("/entradas")
public class EntradaController extends MovimientoControllerBase {

    @Override
    protected String tipo() { return "ENTRADA"; }

    @Override
    protected String ruta() { return "/entradas"; }

    @Override
    protected Movimiento registrar(MovimientoForm form, String username) {
        return movimientoService.registrarEntrada(form, username);
    }
}
