package com.github.kaiwinter.myatmo.chart.rest;

import com.github.kaiwinter.myatmo.chart.rest.model.Measure;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MeasureService {

    @FormUrlEncoded
    @POST("/api/getmeasure")
    Call<Measure> getMeasure(@Header("Authorization") String accessToken, @Field("device_id") String deviceId, @Field("module_id") String moduleId, @Field("scale") String scale, @Field("type") String type, @Field("date_begin") int dateBegin, @Field("optimize") boolean optimize);

}