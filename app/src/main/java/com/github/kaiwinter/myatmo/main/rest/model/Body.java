
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Body {

    @SerializedName("devices")
    public List<Device> devices = null;
    @SerializedName("user")
    public User user;

}
