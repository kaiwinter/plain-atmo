
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

public class StationsData {

    @SerializedName("body")
    public Body body;
    @SerializedName("status")
    public String status;
    @SerializedName("time_exec")
    public Double timeExec;
    @SerializedName("time_server")
    public Integer timeServer;
}
