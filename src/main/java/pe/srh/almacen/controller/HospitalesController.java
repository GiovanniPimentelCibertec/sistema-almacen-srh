package pe.srh.almacen.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.srh.almacen.entity.RegistroError;
import pe.srh.almacen.entity.Sede;
import pe.srh.almacen.service.HospitalService;
import pe.srh.almacen.service.MovimientoException;

/** Vista del jefe de logística: stock y consumo de cada hospital, envíos sin confirmar y diferencias. */
@Controller
@RequestMapping("/hospitales")
public class HospitalesController {

    @Autowired
    private HospitalService hospitalService;

    @GetMapping({"", "/"})
    public String hospitales(@RequestParam(required = false) Integer sede, Model model) {
        List<Sede> hospitales = hospitalService.hospitales();
        // Por defecto se muestra el primer hospital
        Sede elegido = hospitales.stream().filter(s -> s.getIdSede().equals(sede)).findFirst()
                .orElse(hospitales.isEmpty() ? null : hospitales.get(0));

        // Resumen de cada hospital: envíos por confirmar y diferencias por revisar
        Map<Sede, Integer> pendientes = new LinkedHashMap<>();
        Map<Sede, Integer> porRevisar = new LinkedHashMap<>();
        hospitales.forEach(h -> {
            pendientes.put(h, hospitalService.enviosPendientes(h).size());
            porRevisar.put(h, hospitalService.diferenciasPendientes(h).size());
        });

        model.addAttribute("hospitales", hospitales);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("porRevisar", porRevisar);
        model.addAttribute("elegido", elegido);
        if (elegido != null) {
            // Cada pestaña muestra solo las diferencias de su hospital
            model.addAttribute("diferencias", hospitalService.diferenciasPendientes(elegido));
        }
        if (elegido != null && elegido.isControlIniciado()) {
            model.addAttribute("stock", hospitalService.stockDeSede(elegido));
            model.addAttribute("envios", hospitalService.enviosPendientes(elegido));
            model.addAttribute("registros", hospitalService.ultimosRegistros(elegido));
        }
        return "hospitales/inicio";
    }

    /** El jefe marca una diferencia de envío como revisada: APROBADO o RECHAZADO. */
    @PostMapping("/diferencias/{id}/revisar")
    public String revisar(@PathVariable("id") Integer id, @RequestParam(required = false) String estado,
                          @RequestParam(required = false) Integer sede, Authentication auth,
                          RedirectAttributes flash) {
        try {
            RegistroError e = hospitalService.revisarDiferencia(id, estado, auth.getName());
            flash.addFlashAttribute("mensaje", "Diferencia del envío N.° " + e.getMovimiento().getIdMovimiento()
                    + " revisada: " + ("APROBADO".equals(e.getEstado()) ? "aprobada." : "rechazada."));
        } catch (MovimientoException e) {
            flash.addFlashAttribute("error", String.join(" ", e.getErrores()));
        }
        return sede == null ? "redirect:/hospitales" : "redirect:/hospitales?sede=" + sede;
    }
}
