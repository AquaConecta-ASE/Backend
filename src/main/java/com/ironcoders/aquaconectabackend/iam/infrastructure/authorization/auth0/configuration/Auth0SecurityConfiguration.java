package com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.auth0.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class Auth0SecurityConfiguration {

    @Value("${auth0.audience}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // ✅ CORS deshabilitado - El BFF Gateway (puerto 8081) maneja CORS
        // El backend solo recibe requests del gateway (localhost:8081)
        http.cors(cors -> cors.disable());

        // Disable CSRF (stateless API)
        http.csrf(csrf -> csrf.disable());

        // Session management (stateless)
        http.sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // Configure authorization
        http.authorizeHttpRequests(authorize -> authorize
            // Public endpoints
            .requestMatchers(
                "/api/v1/farm/all",
                "/api/v1/farm/**",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/webjars/**"
            ).permitAll()
            // All other requests require authentication
            .anyRequest().authenticated()
        );

        // Configure OAuth2 Resource Server with JWT
        http.oauth2ResourceServer(oauth2 -> 
            oauth2.jwt(jwt -> 
                jwt.decoder(jwtDecoder())
                   .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
        );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        
        // ⚠️ IMPORTANTE: Auth0 enviará los roles en el claim personalizado
        converter.setAuthoritiesClaimName("https://aquaconecta.com/roles");
        converter.setAuthorityPrefix(""); // Sin prefijo porque ya vienen con ROLE_

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        
        return jwtConverter;
    }
}