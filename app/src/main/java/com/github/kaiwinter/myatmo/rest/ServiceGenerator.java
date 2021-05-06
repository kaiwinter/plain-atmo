package com.github.kaiwinter.myatmo.rest;

import com.github.kaiwinter.myatmo.chart.rest.model.Body;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.Reader;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    public static final String API_BASE_URL = "https://api.netatmo.com";

    public static <S> S createService(Class<S> serviceClass) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        BodyTypeAdapter myAdapter = new BodyTypeAdapter();
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Body.class, myAdapter)
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(serviceClass);
    }

    public static APIError parseError(retrofit2.Response<?> response) {
        Gson gson = new Gson();
        Reader reader = response.errorBody().charStream();

        APIError apiError;
        try {
            apiError = gson.fromJson(reader, APIError.class);
        } catch (JsonParseException e) {
            apiError = new APIError();
            apiError.error = new APIError.Error();
            apiError.error.message = e.getMessage();
        }
        return apiError;
    }

}