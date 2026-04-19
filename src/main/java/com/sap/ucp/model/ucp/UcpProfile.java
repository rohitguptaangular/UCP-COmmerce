package com.sap.ucp.model.ucp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class UcpProfile {

    private String version;

    @JsonProperty("business_name")
    private String businessName;

    @JsonProperty("business_id")
    private String businessId;

    private List<ServiceDefinition> services;
    private List<Capability> capabilities;

    @JsonProperty("payment_handlers")
    private List<PaymentHandler> paymentHandlers;

    @JsonProperty("identity_linking")
    private IdentityLinkingConfig identityLinking;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public List<ServiceDefinition> getServices() { return services; }
    public void setServices(List<ServiceDefinition> services) { this.services = services; }
    public List<Capability> getCapabilities() { return capabilities; }
    public void setCapabilities(List<Capability> capabilities) { this.capabilities = capabilities; }
    public List<PaymentHandler> getPaymentHandlers() { return paymentHandlers; }
    public void setPaymentHandlers(List<PaymentHandler> paymentHandlers) { this.paymentHandlers = paymentHandlers; }
    public IdentityLinkingConfig getIdentityLinking() { return identityLinking; }
    public void setIdentityLinking(IdentityLinkingConfig identityLinking) { this.identityLinking = identityLinking; }

    public static class IdentityLinkingConfig {
        @JsonProperty("authorization_endpoint")
        private String authorizationEndpoint;

        @JsonProperty("token_endpoint")
        private String tokenEndpoint;

        private List<String> scopes;

        public String getAuthorizationEndpoint() { return authorizationEndpoint; }
        public void setAuthorizationEndpoint(String authorizationEndpoint) { this.authorizationEndpoint = authorizationEndpoint; }
        public String getTokenEndpoint() { return tokenEndpoint; }
        public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }
        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }
    }
}
