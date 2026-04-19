package com.sap.ucp.model.ucp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class OrderStatus {

    @JsonProperty("order_id")
    private String orderId;

    private String status; // confirmed, processing, shipped, delivered, cancelled, returned

    @JsonProperty("checkout_session_id")
    private String checkoutSessionId;

    private List<CheckoutItem> items;
    private CheckoutSession.PriceSummary pricing;

    @JsonProperty("shipping_address")
    private Address shippingAddress;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("tracking_url")
    private String trackingUrl;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCheckoutSessionId() { return checkoutSessionId; }
    public void setCheckoutSessionId(String checkoutSessionId) { this.checkoutSessionId = checkoutSessionId; }
    public List<CheckoutItem> getItems() { return items; }
    public void setItems(List<CheckoutItem> items) { this.items = items; }
    public CheckoutSession.PriceSummary getPricing() { return pricing; }
    public void setPricing(CheckoutSession.PriceSummary pricing) { this.pricing = pricing; }
    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }
}
