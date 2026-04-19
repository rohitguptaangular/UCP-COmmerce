package com.sap.ucp.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified OAuth 2.0 Identity Linking service.
 * In production, this delegates to SAP Commerce Cloud's built-in OAuth2 provider.
 *
 * Flow: Platform redirects user to /authorize -> user grants access ->
 *       redirect back with auth code -> platform exchanges code for token
 */
@Service
public class IdentityService {

    // auth_code -> user info
    private final Map<String, UserGrant> authCodes = new ConcurrentHashMap<>();

    // access_token -> user info
    private final Map<String, UserGrant> accessTokens = new ConcurrentHashMap<>();

    /**
     * Generate an authorization code (simulates user granting consent).
     * In real SAP Commerce, this would redirect to the SAP login/consent page.
     */
    public AuthCodeResult generateAuthCode(String clientId, String redirectUri, String scope, String state) {
        String code = "sap-auth-" + UUID.randomUUID().toString().substring(0, 12);
        UserGrant grant = new UserGrant(
                "demo-user-001",
                "demo@sapcommerce.com",
                clientId,
                scope,
                redirectUri
        );
        authCodes.put(code, grant);

        return new AuthCodeResult(code, state, redirectUri);
    }

    /**
     * Exchange authorization code for access token.
     * In real SAP Commerce: POST /authorizationserver/oauth/token
     */
    public TokenResult exchangeCodeForToken(String code, String clientId, String clientSecret) {
        UserGrant grant = authCodes.remove(code);
        if (grant == null) {
            throw new IllegalArgumentException("Invalid or expired authorization code");
        }

        String accessToken = "sap-token-" + UUID.randomUUID().toString();
        String refreshToken = "sap-refresh-" + UUID.randomUUID().toString();
        accessTokens.put(accessToken, grant);

        return new TokenResult(accessToken, refreshToken, "Bearer", 3600, grant.scope());
    }

    /**
     * Validate an access token and return user info.
     */
    public UserGrant validateToken(String accessToken) {
        UserGrant grant = accessTokens.get(accessToken);
        if (grant == null) throw new IllegalArgumentException("Invalid access token");
        return grant;
    }

    // --- Value types ---

    public record UserGrant(String userId, String email, String clientId, String scope, String redirectUri) {}

    public record AuthCodeResult(String code, String state, String redirectUri) {
        public String buildRedirectUrl() {
            return redirectUri + "?code=" + code + (state != null ? "&state=" + state : "");
        }
    }

    public record TokenResult(
            String accessToken,
            String refreshToken,
            String tokenType,
            int expiresIn,
            String scope
    ) {}
}
