package com.proyecto.servicios.config;

import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.security.JwtAuthenticationFilter;
import com.proyecto.servicios.security.JwtService;
import com.proyecto.servicios.security.SecurityErrorHandler;
import com.proyecto.servicios.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security para la API REST.
 *
 * <ul>
 *   <li>API sin estado (sin sesión HTTP ni CSRF): cada petición se autentica con un token JWT.</li>
 *   <li>Públicos: login, registro de clientes (onboarding), documentación Swagger y health check.</li>
 *   <li>Consultas masivas (todos los clientes, rango de fechas, cuentas activas): solo rol EJECUTIVO.</li>
 *   <li>Todo lo demás requiere token; la propiedad de cada registro se valida en {@code ControlAcceso}.</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/health/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   AuthService authService,
                                                   SecurityErrorHandler securityErrorHandler) throws Exception {
        String ejecutivo = RolEnum.EJECUTIVO.name();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // Onboarding: alta de clientes. Las banderas 2 y 3 de layaway exigen token en el controlador.
                        .requestMatchers(HttpMethod.POST, "/api/v1/clientes", "/api/v1/layaway/cliente").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/clientes", "/api/v1/clientes/rango-fechas",
                                "/api/v1/cuentas/activas").hasRole(ejecutivo)
                        .requestMatchers("/personas", "/personasActualiza", "/personasElimina").hasRole(ejecutivo)
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, authService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
