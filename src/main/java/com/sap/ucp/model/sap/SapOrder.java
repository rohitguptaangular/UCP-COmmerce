package com.sap.ucp.model.sap;

import java.time.Instant;
import java.util.List;

public class SapOrder {

    private String code;
    private String status; // CREATED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    private String cartCode;
    private List<SapCartEntry> entries;
    private double totalPrice;
    private double deliveryCost;
    private double totalTax;
    private String currency;
    private SapCart.SapAddress deliveryAddress;
    private Instant created;
    private Instant statusUpdated;
    private String trackingNumber;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCartCode() { return cartCode; }
    public void setCartCode(String cartCode) { this.cartCode = cartCode; }
    public List<SapCartEntry> getEntries() { return entries; }
    public void setEntries(List<SapCartEntry> entries) { this.entries = entries; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public double getDeliveryCost() { return deliveryCost; }
    public void setDeliveryCost(double deliveryCost) { this.deliveryCost = deliveryCost; }
    public double getTotalTax() { return totalTax; }
    public void setTotalTax(double totalTax) { this.totalTax = totalTax; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public SapCart.SapAddress getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(SapCart.SapAddress deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public Instant getCreated() { return created; }
    public void setCreated(Instant created) { this.created = created; }
    public Instant getStatusUpdated() { return statusUpdated; }
    public void setStatusUpdated(Instant statusUpdated) { this.statusUpdated = statusUpdated; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
}
