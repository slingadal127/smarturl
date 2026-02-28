package com.smarturl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we're using JWT not sessions
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS for React frontend
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless — no server-side sessions, JWT handles auth
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define which endpoints are public vs protected
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — anyone can access
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/urls/shorten").permitAll()
                        .requestMatchers("/api/v1/urls/health").permitAll()
                        .requestMatchers("/api/v1/urls/*/analytics").permitAll()
                        .requestMatchers("/r/**").permitAll()
                        .requestMatchers("/", "/index.html", "/static/**").permitAll()
                        // Allow all for now during development
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt automatically salts passwords — industry standard
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8082"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        return source -> {
            UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
            s.registerCorsConfiguration("/**", config);
            return s.getCorsConfiguration(source);
        };
    }
}