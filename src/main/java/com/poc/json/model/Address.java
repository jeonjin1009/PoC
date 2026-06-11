package com.poc.json.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Address {

    private String city;
    private String district;

    @JsonProperty("zipCode")
    private String zipCode;

    public Address() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    @Override
    public String toString() {
        return String.format("Address{city='%s', district='%s', zipCode='%s'}", city, district, zipCode);
    }
}
