package pe.srh.almacen.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Se aplica a todos los formularios: quita los espacios del inicio y del final de cada texto
 * (así " EPP " no pasa como una categoría distinta de "EPP") y convierte un texto vacío en null.
 */
@ControllerAdvice
public class FormularioConfig {

    @InitBinder
    public void recortarTextos(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
