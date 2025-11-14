package com.ironcoders.aquaconectabackend.shared.Infrastructure.auth0;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.TokenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class Auth0ManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(Auth0ManagementService.class);
    
    @Value("${auth0.management.domain}")
    private String domain;
    
    // Opción 1: Token estático (desarrollo/testing)
    @Value("${auth0.management.token:}")
    private String managementToken;
    
    // Opción 2: Client credentials (producción - recomendado)
    @Value("${auth0.management.client-id:}")
    private String clientId;
    
    @Value("${auth0.management.client-secret:}")
    private String clientSecret;
    
    @Value("${auth0.management.connection:Username-Password-Authentication}")
    private String connection;
    
    // Cache para el token generado
    private String cachedToken = null;
    private long tokenExpirationTime = 0;
    
    /**
     * Creates ManagementAPI instance using either:
     * 1. Static token (if configured)
     * 2. Client credentials (M2M app) - automatically refreshes tokens
     * 
     * @return ManagementAPI instance
     * @throws IllegalStateException if neither option is configured
     */
    private ManagementAPI getManagementAPI() throws Auth0Exception {
        // Opción 1: Token estático (más simple pero expira en 24h)
        if (managementToken != null && !managementToken.isEmpty()) {
            logger.debug("Using static Management API token");
            return ManagementAPI.newBuilder(domain, managementToken).build();
        } 
        
        // Opción 2: Client credentials (recomendado para producción)
        if (clientId != null && !clientId.isEmpty() && 
            clientSecret != null && !clientSecret.isEmpty()) {
            logger.debug("Using M2M client credentials for Management API");
            
            // Obtener o renovar token
            String token = getOrRefreshToken();
            return ManagementAPI.newBuilder(domain, token).build();
        }
        
        // Error si ninguna opción está configurada
        throw new IllegalStateException(
            "Auth0 Management API not configured. Please provide either:\n" +
            "  - auth0.management.token (for development), or\n" +
            "  - auth0.management.client-id and auth0.management.client-secret (for production)"
        );
    }
    
    /**
     * Obtains or refreshes the Management API access token using client credentials.
     * Tokens are cached and only refreshed when expired.
     * 
     * @return Access token for Management API
     * @throws Auth0Exception if token request fails
     */
    private String getOrRefreshToken() throws Auth0Exception {
        // Si hay token en cache y no ha expirado, usarlo
        long currentTime = System.currentTimeMillis();
        if (cachedToken != null && currentTime < tokenExpirationTime) {
            logger.debug("Using cached Management API token");
            return cachedToken;
        }
        
        // Solicitar nuevo token
        logger.info("Requesting new Management API token using client credentials");
        AuthAPI authAPI = AuthAPI.newBuilder(domain, clientId, clientSecret).build();
        
        TokenRequest tokenRequest = authAPI.requestToken("https://" + domain + "/api/v2/");
        com.auth0.json.auth.TokenHolder tokenHolder = tokenRequest.execute().getBody();
        
        // Guardar en cache (con margen de 5 minutos antes de expiración)
        cachedToken = tokenHolder.getAccessToken();
        Long expiresInLong = tokenHolder.getExpiresIn();
        int expiresIn = (expiresInLong != null) ? expiresInLong.intValue() : 86400; // default 24h
        tokenExpirationTime = currentTime + ((expiresIn - 300) * 1000L); // -5 minutos de margen
        
        logger.info("✅ New Management API token obtained, expires in {} seconds", expiresIn);
        return cachedToken;
    }
    
    /**
     * Creates a new resident user in Auth0 with ROLE_RESIDENT
     * 
     * @param email The resident's email
     * @param firstName The resident's first name
     * @param lastName The resident's last name
     * @param providerId The provider ID to store in app_metadata
     * @return The Auth0 user ID
     * @throws Auth0Exception if the user creation fails
     */
    public String createResidentUser(String email, String firstName, String lastName, Long providerId) 
            throws Auth0Exception {
        
        ManagementAPI mgmt = getManagementAPI();
        
        // Generate temporary secure password (user will change it via email)
        String tempPassword = generateSecurePassword();
        
        // Create user object
        User user = new User();
        user.setEmail(email);
        user.setPassword(tempPassword.toCharArray()); // Required by Auth0
        user.setName(firstName + " " + lastName);
        user.setGivenName(firstName);
        user.setFamilyName(lastName);
        user.setConnection(connection);
        user.setEmailVerified(true); // Skip email verification, will use password reset flow
        
        // Add app_metadata with role and providerId
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("role", "ROLE_RESIDENT");
        if (providerId != null) {
            appMetadata.put("providerId", providerId);
        }
        user.setAppMetadata(appMetadata);
        
        // Create user in Auth0
        User createdUser = mgmt.users().create(user).execute().getBody();
        
        logger.info("✅ Created Auth0 user for resident: {} with ID: {}", email, createdUser.getId());
        
        return createdUser.getId();
    }
    
    /**
     * Creates a new provider user in Auth0 with ROLE_PROVIDER
     * 
     * @param email The provider's email
     * @param firstName The provider's first name  
     * @param lastName The provider's last name
     * @return The Auth0 user ID
     * @throws Auth0Exception if the user creation fails
     */
    public String createProviderUser(String email, String firstName, String lastName) 
            throws Auth0Exception {
        
        ManagementAPI mgmt = getManagementAPI();
        
        // Generate temporary secure password (user will change it via email)
        String tempPassword = generateSecurePassword();
        
        // Create user object
        User user = new User();
        user.setEmail(email);
        user.setPassword(tempPassword.toCharArray()); // Required by Auth0
        user.setName(firstName + " " + lastName);
        user.setGivenName(firstName);
        user.setFamilyName(lastName);
        user.setConnection(connection);
        user.setEmailVerified(true); // Skip email verification, will use password reset flow
        
        // Add app_metadata with role
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("role", "ROLE_PROVIDER");
        user.setAppMetadata(appMetadata);
        
        // Create user in Auth0
        User createdUser = mgmt.users().create(user).execute().getBody();
        
        logger.info("✅ Created Auth0 user for provider: {} with ID: {}", email, createdUser.getId());
        
        return createdUser.getId();
    }
    
    /**
     * Generates a secure random password for temporary user creation.
     * Password format: Uppercase + Lowercase + Digits + Special chars
     * Length: 16 characters
     * 
     * @return A secure random password
     */
    private String generateSecurePassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String allChars = upper + lower + digits + special;
        
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder(16);
        
        // Ensure at least one of each type
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));
        
        // Fill remaining characters randomly
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
    
    /**
     * Sends a password setup email to the user
     * Creates a password change ticket that Auth0 will send via email
     * 
     * @param auth0UserId The Auth0 user ID (e.g., "auth0|123456...")
     * @return The password reset URL that was sent via email
     * @throws Auth0Exception if the email sending fails
     */
    public String sendPasswordSetupEmail(String auth0UserId) throws Auth0Exception {
        ManagementAPI mgmt = getManagementAPI();
        
        // Create password change ticket with user ID (not email!)
        com.auth0.json.mgmt.tickets.PasswordChangeTicket ticket = 
            new com.auth0.json.mgmt.tickets.PasswordChangeTicket(auth0UserId);
        
        // Request password change from Auth0
        com.auth0.json.mgmt.tickets.PasswordChangeTicket result = 
            mgmt.tickets().requestPasswordChange(ticket).execute().getBody();
        
        String resetUrl = result.getTicket();
        
        logger.info("✅ Password change email sent for user: {}", auth0UserId);
        logger.info("🔗 Password reset URL: {}", resetUrl);
        
        return resetUrl;
    }
    
    /**
     * Deletes a user from Auth0
     * Used for cleanup if resident creation fails
     * 
     * @param auth0UserId The Auth0 user ID
     * @throws Auth0Exception if the deletion fails
     */
    public void deleteUser(String auth0UserId) throws Auth0Exception {
        ManagementAPI mgmt = getManagementAPI();
        mgmt.users().delete(auth0UserId).execute();
        logger.info("Deleted Auth0 user: {}", auth0UserId);
    }
    
    /**
     * Gets user information from Auth0
     * 
     * @param auth0UserId The Auth0 user ID
     * @return The Auth0 user object
     * @throws Auth0Exception if the user is not found
     */
    public User getUser(String auth0UserId) throws Auth0Exception {
        ManagementAPI mgmt = getManagementAPI();
        return mgmt.users().get(auth0UserId, null).execute().getBody();
    }
    
    /**
     * Updates user app_metadata
     * Used to link Auth0 user with resident ID after creation
     * 
     * @param auth0UserId The Auth0 user ID
     * @param residentId The resident ID to store
     * @throws Auth0Exception if the update fails
     */
    public void linkResidentToUser(String auth0UserId, Long residentId) throws Auth0Exception {
        ManagementAPI mgmt = getManagementAPI();
        
        User user = new User();
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("residentId", residentId);
        user.setAppMetadata(appMetadata);
        
        mgmt.users().update(auth0UserId, user).execute();
        
        logger.info("Linked Auth0 user {} with resident ID: {}", auth0UserId, residentId);
    }
}
