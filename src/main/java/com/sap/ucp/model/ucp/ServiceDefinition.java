package com.sap.ucp.model.ucp;

public class ServiceDefinition {

    private String transport;
    private String endpoint;
    private String spec;

    public ServiceDefinition() {}

    public ServiceDefinition(String transport, String endpoint, String spec) {
        this.transport = transport;
        this.endpoint = endpoint;
        this.spec = spec;
    }

    public String getTransport() { return transport; }
    public void setTransport(String transport) { this.transport = transport; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
}
