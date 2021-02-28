
package com.github.kaiwinter.myatmo.main.rest.model;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("mail")
    public String mail;
    @SerializedName("administrative")
    public Administrative administrative;
}
