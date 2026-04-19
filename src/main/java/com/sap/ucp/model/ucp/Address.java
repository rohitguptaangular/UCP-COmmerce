package com.sap.ucp.model.ucp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Address {

    private String name;

    @JsonProperty("street_address")
    private String streetAddress;

    private String city;
    private String state;

    @JsonProperty("postal_code")
    private String postalCode;

    private String country;
    private String phone;

    public Address() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
