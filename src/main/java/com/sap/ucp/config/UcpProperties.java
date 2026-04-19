package com.sap.ucp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ucp")
public class UcpProperties {

    private Merchant merchant = new Merchant();
    private Sap sap = new Sap();

    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }
    public Sap getSap() { return sap; }
    public void setSap(Sap sap) { this.sap = sap; }

    public static class Merchant {
        private String name = "SAP Commerce Demo Store";
        private String id = "sap-commerce-demo";
        private String baseUrl = "http://localhost:8080";
        private String version = "2026-01-11";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    public static class Sap {
        private String occBaseUrl = "https://sap-commerce.example.com/occ/v2/electronics";
        private String siteId = "electronics";

        public String getOccBaseUrl() { return occBaseUrl; }
        public void setOccBaseUrl(String occBaseUrl) { this.occBaseUrl = occBaseUrl; }
        public String getSiteId() { return siteId; }
        public void setSiteId(String siteId) { this.siteId = siteId; }
    }
}
