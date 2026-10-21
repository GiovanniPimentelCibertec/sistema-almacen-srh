package pe.srh.almacen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.srh.almacen.service.HospitalService;

/**
 * Al arrancar el sistema, los hospitales que aún no registran su stock empiezan hoy con todo en 0.
 * Desde ese día su stock sube con los envíos confirmados y baja con su consumo.
 */
@Component
public class InicioControlHospitales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicioControlHospitales.class);

    private final HospitalService hospitalService;

    public InicioControlHospitales(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int iniciados = hospitalService.iniciarHospitalesPendientes();
        if (iniciados > 0) {
            log.info("{} hospital(es) empiezan hoy su control de stock en 0", iniciados);
        }
    }
}
