package com.sap.ucp.model.sap;

public class SapCartEntry {

    private int entryNumber;
    private String productCode;
    private String productName;
    private int quantity;
    private double basePrice;
    private double totalPrice;
    private String currency;

    public SapCartEntry() {}

    public int getEntryNumber() { return entryNumber; }
    public void setEntryNumber(int entryNumber) { this.entryNumber = entryNumber; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
