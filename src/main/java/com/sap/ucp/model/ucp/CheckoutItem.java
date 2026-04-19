package com.sap.ucp.model.ucp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckoutItem {

    @JsonProperty("product_id")
    private String productId;

    private String name;
    private int quantity;

    @JsonProperty("unit_price")
    private double unitPrice;

    private String currency;

    @JsonProperty("image_url")
    private String imageUrl;

    public CheckoutItem() {}

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
