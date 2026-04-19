package com.sap.ucp.model.sap;

import java.util.ArrayList;
import java.util.List;

public class SapCart {

    private String code;
    private String guid;
    private List<SapCartEntry> entries = new ArrayList<>();
    private double subTotal;
    private double deliveryCost;
    private double totalTax;
    private double totalPrice;
    private String currency;
    private String deliveryMode;
    private SapAddress deliveryAddress;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    public List<SapCartEntry> getEntries() { return entries; }
    public void setEntries(List<SapCartEntry> entries) { this.entries = entries; }
    public double getSubTotal() { return subTotal; }
    public void setSubTotal(double subTotal) { this.subTotal = subTotal; }
    public double getDeliveryCost() { return deliveryCost; }
    public void setDeliveryCost(double deliveryCost) { this.deliveryCost = deliveryCost; }
    public double getTotalTax() { return totalTax; }
    public void setTotalTax(double totalTax) { this.totalTax = totalTax; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(String deliveryMode) { this.deliveryMode = deliveryMode; }
    public SapAddress getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(SapAddress deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public static class SapAddress {
        private String firstName;
        private String lastName;
        private String line1;
        private String town;
        private String region;
        private String postalCode;
        private String country;
        private String phone;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getLine1() { return line1; }
        public void setLine1(String line1) { this.line1 = line1; }
        public String getTown() { return town; }
        public void setTown(String town) { this.town = town; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
