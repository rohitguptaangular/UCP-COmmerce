package com.sap.ucp.controller;

import com.sap.ucp.service.IdentityService;
import com.sap.ucp.service.IdentityService.AuthCodeResult;
import com.sap.ucp.service.IdentityService.TokenResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * UCP Identity Linking Capability — OAuth 2.0 endpoints.
 *
 * Enables AI platforms to obtain authorization to perform actions on a user's behalf.
 * In production, this delegates to SAP Commerce Cloud's OAuth2 authorization server
 * at /authorizationserver/oauth/authorize and /authorizationserver/oauth/token.
 *
 * Flow:
 * 1. Platform redirects user to /authorize with client_id, redirect_uri, scope
 * 2. User authenticates on SAP Commerce login page (simulated in POC)
 * 3. SAP redirects back to platform with authorization code
 * 4. Platform exchanges code for access token via /token
 * 5. Platform uses token for authenticated checkout/order operations
 */
@RestController
@RequestMapping("/ucp/api/identity")
public class IdentityLinkingController {

    private final IdentityService identityService;

    public IdentityLinkingController(IdentityService identityService) {
        this.identityService = identityService;
    }

    /**
     * OAuth 2.0 Authorization endpoint.
     *
     * In real SAP Commerce: GET /authorizationserver/oauth/authorize
     * For POC: auto-grants consent and redirects with auth code.
     */
    @GetMapping("/authorize")
    public ResponseEntity<Object> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", defaultValue = "checkout order.read") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "response_type", defaultValue = "code") String responseType) {

        AuthCodeResult result = identityService.generateAuthCode(clientId, redirectUri, scope, state);

        // In a real implementation, this would show a login/consent page first.
        // For POC, we auto-approve and redirect.
        return ResponseEntity.status(302)
                .location(URI.create(result.buildRedirectUrl()))
                .body(Map.of(
                        "message", "Redirecting to platform with auth code",
                        "code", result.code(),
                        "redirect_url", result.buildRedirectUrl()
                ));
    }

    /**
     * OAuth 2.0 Token endpoint.
     *
     * In real SAP Commerce: POST /authorizationserver/oauth/token
     * Exchanges authorization code for access + refresh tokens.
     */
    @PostMapping("/token")
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("code") String code,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "client_secret", defaultValue = "") String clientSecret) {

        if (!"authorization_code".equals(grantType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "unsupported_grant_type"));
        }

        try {
            TokenResult tokenResult = identityService.exchangeCodeForToken(code, clientId, clientSecret);
            return ResponseEntity.ok(Map.of(
                    "access_token", tokenResult.accessToken(),
                    "refresh_token", tokenResult.refreshToken(),
                    "token_type", tokenResult.tokenType(),
                    "expires_in", tokenResult.expiresIn(),
                    "scope", tokenResult.scope()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_grant", "message", e.getMessage()));
        }
    }

    /**
     * User info endpoint (bonus — useful for verifying token works).
     * In real SAP Commerce: GET /occ/v2/{site}/users/current
     */
    @GetMapping("/userinfo")
    public ResponseEntity<?> userInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            var grant = identityService.validateToken(token);
            return ResponseEntity.ok(Map.of(
                    "user_id", grant.userId(),
                    "email", grant.email(),
                    "scope", grant.scope()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "invalid_token"));
        }
    }
}
