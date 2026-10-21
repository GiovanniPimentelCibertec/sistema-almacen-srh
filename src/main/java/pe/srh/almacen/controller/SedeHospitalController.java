package pe.srh.almacen.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import pe.srh.almacen.entity.ConsumoSede;
import pe.srh.almacen.entity.Movimiento;
import pe.srh.almacen.entity.Sede;
import pe.srh.almacen.service.HospitalService;
import pe.srh.almacen.service.MovimientoException;

/** Pantallas del encargado de un hospital (rol SEDE): su stock, envíos por confirmar y consumo. */
@Controller
@RequestMapping("/sede")
public class SedeHospitalController {

    @Autowired
    private HospitalService hospitalService;

    /** Panel del hospital: envíos por confirmar, stock y últimos registros. */
    @GetMapping({"", "/"})
    public String panel(Authentication auth, Model model) {
        Sede sede = hospitalService.sedeDe(auth.getName());
        model.addAttribute("sede", sede);
        model.addAttribute("envios", hospitalService.enviosPendientes(sede));
        model.addAttribute("stock", hospitalService.stockDeSede(sede));
        model.addAttribute("registros", hospitalService.ultimosRegistros(sede));
        return "sede/panel";
    }

    // ---------- Consumo por turno (CU-12) ----------

    @GetMapping("/consumo")
    public String consumo(Authentication auth, Model model) {
        Sede sede = hospitalService.sedeDe(auth.getName());
        return mostrarConsumo(sede, null, null, null, model);
    }

    @PostMapping("/consumo")
    public String guardarConsumo(@RequestParam(required = false) String idProducto,
                                 @RequestParam(required = false) String cantidad,
                                 Authentication auth, Model model, RedirectAttributes flash) {
        Sede sede = hospitalService.sedeDe(auth.getName());
        Integer id, cant = null;
        try {
            id = idProducto == null || idProducto.isBlank() ? null : Integer.valueOf(idProducto.trim());
        } catch (NumberFormatException e) {
            return mostrarConsumo(sede, List.of("Seleccione un producto de la lista."), null, null, model);
        }
        try {
            cant = cantidad == null || cantidad.isBlank() ? null : Integer.valueOf(cantidad.trim());
            ConsumoSede c = hospitalService.registrarConsumo(sede.getIdSede(), id, cant, auth.getName());
            int queda = hospitalService.stockDeProducto(sede, id);
            flash.addFlashAttribute("mensaje", "Consumo registrado: " + c.getCantidad() + " " + c.getProducto().getUnidad()
                    + " de " + c.getProducto().getNombre() + " (turno " + c.getTurnoNombre() + "). Quedan "
                    + queda + " " + c.getProducto().getUnidad() + ".");
        } catch (NumberFormatException e) {
            return mostrarConsumo(sede, List.of("La cantidad debe ser un número entero."), id, null, model);
        } catch (MovimientoException e) {
            return mostrarConsumo(sede, e.getErrores(), id, cant, model);
        }
        return "redirect:/sede/consumo";
    }

    private String mostrarConsumo(Sede sede, List<String> errores, Integer idProducto, Integer cantidad, Model model) {
        model.addAttribute("sede", sede);
        model.addAttribute("errores", errores);
        model.addAttribute("idProducto", idProducto);
        model.addAttribute("cantidad", cantidad);
        model.addAttribute("turno", ConsumoSede.nombreTurno(HospitalService.turnoPara(LocalTime.now())));
        // Solo se ofrecen los productos que el hospital tiene
        model.addAttribute("stock", hospitalService.stockDeSede(sede).stream().filter(f -> f.getStock() > 0).toList());
        return "sede/consumo";
    }

    // ---------- Confirmar envío (CU-13) ----------

    @GetMapping("/envios/{id}")
    public String envio(@PathVariable("id") Integer id, Authentication auth, Model model) {
        Sede sede = hospitalService.sedeDe(auth.getName());
        Movimiento envio = buscarEnvioPendiente(sede, id);
        if (envio == null) {
            return "redirect:/sede";
        }
        model.addAttribute("sede", sede);
        model.addAttribute("envio", envio);
        return "sede/envio";
    }

    @PostMapping("/envios/{id}/conforme")
    public String conforme(@PathVariable("id") Integer id, Authentication auth, RedirectAttributes flash) {
        Sede sede = hospitalService.sedeDe(auth.getName());
        try {
            hospitalService.confirmarEnvio(sede.getIdSede(), id, null, auth.getName());
            flash.addFlashAttribute("mensaje", "Envío N.° " + id + " confirmado: llegó completo y ya está en su stock.");
        } catch (MovimientoException e) {
            flash.addFlashAttribute("error", String.join(" ", e.getErrores()));
        }
        return "redirect:/sede";
    }

    @PostMapping("/envios/{id}/diferente")
    public String diferente(@PathVariable("id") Integer id, @RequestParam Map<String, String> datos,
                            Authentication auth, Model model, RedirectAttributes flash) {
        Sede sede = hospitalService.sedeDe(auth.getName());
        Map<Integer, Integer> llego = new HashMap<>();
        try {
            // Los campos llegan como llego_<idDetalle>
            datos.forEach((k, v) -> {
                if (k.startsWith("llego_")) {
                    llego.put(Integer.valueOf(k.substring(6)), v == null || v.isBlank() ? null : Integer.valueOf(v.trim()));
                }
            });
            boolean conforme = hospitalService.confirmarEnvio(sede.getIdSede(), id, llego, auth.getName());
            flash.addFlashAttribute("mensaje", conforme
                    ? "Envío N.° " + id + " confirmado: las cantidades coinciden, llegó completo."
                    : "Envío N.° " + id + " registrado con diferencia. Su stock tiene lo que realmente llegó y el jefe de logística revisará lo faltante.");
            return "redirect:/sede";
        } catch (NumberFormatException e) {
            model.addAttribute("errores", List.of("Las cantidades deben ser números enteros."));
        } catch (MovimientoException e) {
            model.addAttribute("errores", e.getErrores());
        }
        Movimiento envio = buscarEnvioPendiente(sede, id);
        if (envio == null) {
            return "redirect:/sede";
        }
        model.addAttribute("sede", sede);
        model.addAttribute("envio", envio);
        model.addAttribute("diferente", true);
        return "sede/envio";
    }

    /** Envío pendiente de confirmar del propio hospital, o null si no existe o es de otra sede. */
    private Movimiento buscarEnvioPendiente(Sede sede, Integer id) {
        return hospitalService.enviosPendientes(sede).stream()
                .filter(m -> m.getIdMovimiento().equals(id)).findFirst().orElse(null);
    }
}
