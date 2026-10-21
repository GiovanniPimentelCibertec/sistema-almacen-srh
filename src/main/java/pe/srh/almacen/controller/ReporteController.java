package pe.srh.almacen.controller;

import java.time.YearMonth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.srh.almacen.service.ReporteService;

/** Reportes PDF para el jefe de logística (CU-10). */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping({"", "/"})
    public String inicio(Model model) {
        model.addAttribute("mesActual", YearMonth.now().minusMonths(1));
        return "reportes/inicio";
    }

    // Despachos del almacén a cada sede (la ruta se mantiene como /consumo)
    @GetMapping("/consumo")
    public ResponseEntity<byte[]> consumo(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                                          @RequestParam(defaultValue = "false") boolean descargar,
                                          Authentication auth) {
        return pdf(reporteService.consumoPorSede(mes, auth.getName()), "despachos_sede_" + mes + ".pdf", descargar);
    }

    @GetMapping("/reponer")
    public ResponseEntity<byte[]> reponer(@RequestParam(defaultValue = "false") boolean descargar, Authentication auth) {
        return pdf(reporteService.productosPorReponer(auth.getName()), "productos_por_reponer.pdf", descargar);
    }

    // Se muestra en el navegador o se descarga, según lo que elija el jefe
    private ResponseEntity<byte[]> pdf(byte[] contenido, String archivo, boolean descargar) {
        ContentDisposition cd = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(archivo).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .body(contenido);
    }
}
