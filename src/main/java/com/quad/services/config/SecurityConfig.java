package com.quad.services.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    @Value("${api.version.prefix:/v1}")
    private String apiVersionPrefix;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection disabled for stateless JWT API
                // Rationale: Clients authenticate via Authorization header, not cookies
                // CSRF attacks require browser to auto-send credentials (cookies)
                // JWT tokens in headers are NOT auto-sent by browsers
                // If adding cookie-based auth in future, re-enable CSRF for those routes
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(apiVersionPrefix + "/auth/**").permitAll()  // Authentication
                        .requestMatchers(apiVersionPrefix + "/users/**").permitAll()  // User lookup for OAuth
                        .requestMatchers(apiVersionPrefix + "/agent-rules/**").permitAll()  // Agent rules for VS Code extension
                        .requestMatchers("/health").permitAll()  // Health check (no version)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()  // Swagger UI
                        .requestMatchers("/v3/api-docs/**").permitAll()  // OpenAPI JSON spec
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Read allowed origins from environment-specific properties (dev/qa/prod)
        // DEV/QA: allows all (*), PROD: whitelist only
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
