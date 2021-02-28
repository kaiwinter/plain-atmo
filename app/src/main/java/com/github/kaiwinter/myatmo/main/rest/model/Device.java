
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Device {

    @SerializedName("_id")
    public String id;
    @SerializedName("station_name")
    public String stationName;
    @SerializedName("date_setup")
    public Integer dateSetup;
    @SerializedName("last_setup")
    public Integer lastSetup;
    @SerializedName("type")
    public String type;
    @SerializedName("last_status_store")
    public Integer lastStatusStore;
    @SerializedName("module_name")
    public String moduleName;
    @SerializedName("firmware")
    public Integer firmware;
    @SerializedName("last_upgrade")
    public Integer lastUpgrade;
    @SerializedName("wifi_status")
    public Integer wifiStatus;
    @SerializedName("reachable")
    public Boolean reachable;
    @SerializedName("co2_calibrating")
    public Boolean co2Calibrating;
    @SerializedName("data_type")
    public List<String> dataType = null;
    @SerializedName("place")
    public Place place;
    @SerializedName("home_id")
    public String homeId;
    @SerializedName("home_name")
    public String homeName;
    @SerializedName("dashboard_data")
    public DashboardData dashboardData;
    @SerializedName("modules")
    public List<Module> modules = null;
}
