package com.github.kaiwinter.myatmo.main.rest;

import com.github.kaiwinter.myatmo.main.rest.model.StationsData;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface StationsDataService {

    @FormUrlEncoded
    @POST("/api/getstationsdata")
    Call<StationsData> getStationsData(@Field("device_id") String deviceId);
}