
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Module {

    @SerializedName("_id")
    public String id;
    @SerializedName("type")
    public String type;
    @SerializedName("module_name")
    public String moduleName;
    @SerializedName("last_setup")
    public Integer lastSetup;
    @SerializedName("data_type")
    public List<String> dataType = null;
    @SerializedName("battery_percent")
    public Integer batteryPercent;
    @SerializedName("reachable")
    public Boolean reachable;
    @SerializedName("firmware")
    public Integer firmware;
    @SerializedName("last_message")
    public Integer lastMessage;
    @SerializedName("last_seen")
    public Integer lastSeen;
    @SerializedName("rf_status")
    public Integer rfStatus;
    @SerializedName("battery_vp")
    public Integer batteryVp;
    @SerializedName("dashboard_data")
    public DashboardData dashboardData;
}
