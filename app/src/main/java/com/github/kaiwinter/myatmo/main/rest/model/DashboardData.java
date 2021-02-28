
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

public class DashboardData {

    @SerializedName("time_utc")
    public Integer timeUtc;
    @SerializedName("Temperature")
    public Double temperature;
    @SerializedName("CO2")
    public Integer cO2;
    @SerializedName("Humidity")
    public Integer humidity;
    @SerializedName("Noise")
    public Integer noise;
    @SerializedName("Pressure")
    public Double pressure;
    @SerializedName("AbsolutePressure")
    public Double absolutePressure;
    @SerializedName("min_temp")
    public Double minTemp;
    @SerializedName("max_temp")
    public Double maxTemp;
    @SerializedName("date_max_temp")
    public Integer dateMaxTemp;
    @SerializedName("date_min_temp")
    public Integer dateMinTemp;
    @SerializedName("temp_trend")
    public String tempTrend;
    @SerializedName("pressure_trend")
    public String pressureTrend;
}
