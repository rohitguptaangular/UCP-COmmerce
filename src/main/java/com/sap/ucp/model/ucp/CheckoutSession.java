package com.sap.ucp.model.ucp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CheckoutSession {

    private String id;
    private String status; // incomplete, ready, complete, expired
    private List<CheckoutItem> items;

    @JsonProperty("shipping_address")
    private Address shippingAddress;

    @JsonProperty("delivery_method")
    private String deliveryMethod;

    private PriceSummary pricing;
    private PaymentInfo payment;

    @JsonProperty("order_id")
    private String orderId;

    private UcpMeta ucp;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<CheckoutItem> getItems() { return items; }
    public void setItems(List<CheckoutItem> items) { this.items = items; }
    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    public PriceSummary getPricing() { return pricing; }
    public void setPricing(PriceSummary pricing) { this.pricing = pricing; }
    public PaymentInfo getPayment() { return payment; }
    public void setPayment(PaymentInfo payment) { this.payment = payment; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public UcpMeta getUcp() { return ucp; }
    public void setUcp(UcpMeta ucp) { this.ucp = ucp; }

    public static class PriceSummary {
        private double subtotal;
        private double shipping;
        private double tax;
        private double total;
        private String currency;

        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
        public double getShipping() { return shipping; }
        public void setShipping(double shipping) { this.shipping = shipping; }
        public double getTax() { return tax; }
        public void setTax(double tax) { this.tax = tax; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class PaymentInfo {
        private List<PaymentHandler> handlers;

        public List<PaymentHandler> getHandlers() { return handlers; }
        public void setHandlers(List<PaymentHandler> handlers) { this.handlers = handlers; }
    }

    public static class UcpMeta {
        private String version;
        private List<Capability> capabilities;

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public List<Capability> getCapabilities() { return capabilities; }
        public void setCapabilities(List<Capability> capabilities) { this.capabilities = capabilities; }
    }
}
