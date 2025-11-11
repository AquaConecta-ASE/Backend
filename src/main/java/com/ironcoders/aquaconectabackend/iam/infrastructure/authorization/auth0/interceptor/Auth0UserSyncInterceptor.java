package com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.auth0.interceptor;

import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.auth0.services.Auth0UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que sincroniza automáticamente los usuarios de Auth0 con la base de datos local
 * 
 * Cuando un usuario se autentica con un JWT de Auth0:
 * 1. Extrae la información del token (auth0Id, email, roles)
 * 2. Busca o crea el usuario en la BD local
 * 3. Actualiza los roles si han cambiado
 * 
 * Esto permite que tu backend tenga una copia local de los usuarios para:
 * - Referencias FK en otras tablas (Resident, Provider, etc.)
 * - Queries más rápidas sin llamar a Auth0
 * - Mantener histórico incluso si el usuario es borrado de Auth0
 */
@Component
public class Auth0UserSyncInterceptor implements HandlerInterceptor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Auth0UserSyncInterceptor.class);
    
    private final Auth0UserService auth0UserService;

    public Auth0UserSyncInterceptor(Auth0UserService auth0UserService) {
        this.auth0UserService = auth0UserService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LOGGER.info("🔍 Interceptor ejecutado para: {} {}", request.getMethod(), request.getRequestURI());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            LOGGER.warn("⚠️ No hay autenticación en SecurityContext");
            return true;
        }
        
        LOGGER.info("🔐 Autenticación encontrada: {} (autenticado: {})", 
                    authentication.getClass().getSimpleName(), 
                    authentication.isAuthenticated());
        
        // Solo sincronizar si hay un usuario autenticado con JWT
        if (authentication.isAuthenticated() && 
            authentication instanceof JwtAuthenticationToken) {
            
            LOGGER.info("🎫 Token JWT detectado, iniciando sincronización...");
            
            try {
                // Sincronizar usuario de Auth0 con BD local
                auth0UserService.syncUserFromAuth0(authentication);
                LOGGER.info("✅ Usuario sincronizado desde Auth0 exitosamente");
            } catch (Exception e) {
                LOGGER.error("❌ Error sincronizando usuario de Auth0: {}", e.getMessage(), e);
                // Continuar con el request aunque falle la sincronización
                // (El usuario puede seguir autenticado pero sin datos locales)
            }
        } else {
            LOGGER.warn("⚠️ Autenticación no es JWT o no está autenticado");
        }
        
        return true;
    }
}
