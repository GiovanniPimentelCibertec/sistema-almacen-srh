package pe.srh.almacen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(out -> out
                .logoutSuccessUrl("/login?salir")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/?sinPermiso"));
        return http.build();
    }
}
