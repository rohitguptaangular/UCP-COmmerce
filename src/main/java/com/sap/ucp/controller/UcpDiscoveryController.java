package com.sap.ucp.controller;

import com.sap.ucp.config.UcpProperties;
import com.sap.ucp.model.ucp.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * UCP Discovery Endpoint.
 *
 * Serves the merchant's UCP profile at /.well-known/ucp so that AI agents
 * and platforms can discover capabilities, endpoints, and payment handlers.
 *
 * Equivalent concept in SAP Commerce: The OCC API itself serves as the discovery
 * mechanism, but UCP standardizes this across all commerce platforms.
 */
@RestController
public class UcpDiscoveryController {

    private final UcpProperties properties;

    public UcpDiscoveryController(UcpProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/.well-known/ucp")
    public UcpProfile getProfile() {
        UcpProfile profile = new UcpProfile();
        profile.setVersion(properties.getMerchant().getVersion());
        profile.setBusinessName(properties.getMerchant().getName());
        profile.setBusinessId(properties.getMerchant().getId());

        // Service definitions — REST transport pointing to our facade endpoints
        ServiceDefinition restService = new ServiceDefinition(
                "REST",
                properties.getMerchant().getBaseUrl() + "/ucp/api",
                properties.getMerchant().getBaseUrl() + "/ucp/openapi.json"
        );
        profile.setServices(List.of(restService));

        // Supported capabilities
        String version = properties.getMerchant().getVersion();
        profile.setCapabilities(List.of(
                new Capability("dev.ucp.shopping.checkout", version),
                new Capability("dev.ucp.shopping.order", version),
                new Capability("dev.ucp.identity_linking", version)
        ));

        // Payment handlers
        profile.setPaymentHandlers(List.of(
                new PaymentHandler(
                        "google-pay",
                        "Google Pay",
                        Map.of(
                                "merchant_id", properties.getMerchant().getId(),
                                "gateway", "stripe",
                                "gateway_merchant_id", "sap-demo-stripe-id"
                        ),
                        List.of("CARD", "TOKENIZED_CARD")
                ),
                new PaymentHandler(
                        "credit-card",
                        "Credit Card",
                        Map.of("gateway", "stripe"),
                        List.of("CARD")
                )
        ));

        // Identity linking configuration
        UcpProfile.IdentityLinkingConfig identityConfig = new UcpProfile.IdentityLinkingConfig();
        identityConfig.setAuthorizationEndpoint(properties.getMerchant().getBaseUrl() + "/ucp/api/identity/authorize");
        identityConfig.setTokenEndpoint(properties.getMerchant().getBaseUrl() + "/ucp/api/identity/token");
        identityConfig.setScopes(List.of("checkout", "order.read", "profile.read"));
        profile.setIdentityLinking(identityConfig);

        return profile;
    }
}
