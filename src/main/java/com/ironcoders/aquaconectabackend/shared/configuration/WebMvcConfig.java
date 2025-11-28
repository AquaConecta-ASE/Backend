package com.ironcoders.aquaconectabackend.shared.configuration;

import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.auth0.interceptor.Auth0UserSyncInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Auth0UserSyncInterceptor auth0UserSyncInterceptor;

    public WebMvcConfig(Auth0UserSyncInterceptor auth0UserSyncInterceptor) {
        this.auth0UserSyncInterceptor = auth0UserSyncInterceptor;
    }

    // ✅ CORS COMPLETAMENTE ELIMINADO
    // El BFF Gateway (puerto 8081) maneja 100% de CORS
    // Este backend NO debe enviar ningún header CORS

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auth0UserSyncInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/farm/all", "/api/v1/farm/**", "/v3/api-docs/**", "/swagger-ui/**");
    }
}
