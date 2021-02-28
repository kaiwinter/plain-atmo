
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

public class Administrative {

    @SerializedName("lang")
    public String lang;
    @SerializedName("reg_locale")
    public String regLocale;
    @SerializedName("country")
    public String country;
    @SerializedName("unit")
    public Integer unit;
    @SerializedName("windunit")
    public Integer windunit;
    @SerializedName("pressureunit")
    public Integer pressureunit;
    @SerializedName("feel_like_algo")
    public Integer feelLikeAlgo;

}
