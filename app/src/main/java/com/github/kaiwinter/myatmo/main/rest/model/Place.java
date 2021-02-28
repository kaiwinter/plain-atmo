
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Place {

    @SerializedName("altitude")
    public Integer altitude;
    @SerializedName("city")
    public String city;
    @SerializedName("country")
    public String country;
    @SerializedName("timezone")
    public String timezone;
    @SerializedName("location")
    public List<Double> location = null;
}
