package pe.srh.almacen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

/**
 * Seguridad del sistema: login, cierre de sesión y permisos por rol.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**").permitAll()
                // Solo el administrador
                .requestMatchers("/usuarios/**", "/sedes/**", "/categorias/**").hasRole("ADMINISTRADOR")
                // Marcas y productos: el jefe los gestiona; el asistente solo ve productos
                .requestMatchers("/marcas/**").hasRole("JEFE")
                .requestMatchers("/productos", "/productos/").hasAnyRole("JEFE", "ASISTENTE")
                .requestMatchers("/productos/**").hasRole("JEFE")
                // Entradas y salidas las registran el jefe y el asistente; ajustes y reportes, solo el jefe
                .requestMatchers("/entradas/**", "/salidas/**").hasAnyRole("JEFE", "ASISTENTE")
                .requestMatchers("/ajustes/**", "/reportes/**").hasRole("JEFE")
                // Módulo de hospitales: cada encargado opera su sede; el jefe ve todas
                .requestMatchers("/sede/**").hasRole("SEDE")
                .requestMatchers("/hospitales/**").hasRole("JEFE")
                // Consultas de kardex y stock para el personal del almacén central
                .requestMatchers("/kardex/**", "/stock/**").hasAnyRole("ADMINISTRADOR", "JEFE", "ASISTENTE")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(out -> out
                .logoutSuccessUrl("/login?salir")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedHandler(accesoDenegado()));
        return http.build();
    }

    /**
     * Sin permiso: una pantalla (GET) muestra el inicio con el aviso y estado 403. Un formulario (POST)
     * se redirige al inicio, porque reenviar un POST a "/" daría error 405. Si lo que falló fue el token
     * CSRF (por ejemplo, un formulario abierto antes de volver a iniciar sesión), se avisa que expiró.
     */
    private AccessDeniedHandler accesoDenegado() {
        AccessDeniedHandlerImpl pantalla = new AccessDeniedHandlerImpl();
        pantalla.setErrorPage("/?sinPermiso");
        return (request, response, excepcion) -> {
            if (excepcion instanceof CsrfException) {
                response.sendRedirect(request.getContextPath() + "/?expirado");
            } else if ("GET".equalsIgnoreCase(request.getMethod())) {
                pantalla.handle(request, response, excepcion);
            } else {
                response.sendRedirect(request.getContextPath() + "/?sinPermiso");
            }
        };
    }
}
