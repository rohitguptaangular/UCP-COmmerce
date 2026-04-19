package com.sap.ucp.model.ucp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class PaymentHandler {

    @JsonProperty("handler_id")
    private String handlerId;

    private String name;
    private Map<String, Object> config;

    @JsonProperty("instrument_schemas")
    private List<String> instrumentSchemas;

    public PaymentHandler() {}

    public PaymentHandler(String handlerId, String name, Map<String, Object> config, List<String> instrumentSchemas) {
        this.handlerId = handlerId;
        this.name = name;
        this.config = config;
        this.instrumentSchemas = instrumentSchemas;
    }

    public String getHandlerId() { return handlerId; }
    public void setHandlerId(String handlerId) { this.handlerId = handlerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public List<String> getInstrumentSchemas() { return instrumentSchemas; }
    public void setInstrumentSchemas(List<String> instrumentSchemas) { this.instrumentSchemas = instrumentSchemas; }
}
