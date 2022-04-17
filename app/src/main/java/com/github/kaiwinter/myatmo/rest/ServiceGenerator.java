package com.github.kaiwinter.myatmo.rest;

import com.github.kaiwinter.myatmo.BuildConfig;
import com.github.kaiwinter.myatmo.chart.rest.model.Body;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.Reader;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    public static final String API_BASE_URL = "https://api.netatmo.com";

    public static <S> S createService(Class<S> serviceClass) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(interceptor);
        }

        OkHttpClient client = clientBuilder.build();

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

    /**
     * Parses the error body of Oauth2 related REST responses.
     * <pre>
     *  {
     *     "error": "Request error type",
     *     "error_description": "Request error desc"
     *  }
     * </pre>
     *
     * @param response REST response
     * @return an parsed error object
     */
    public static RestError.OauthError parseOauthError(retrofit2.Response<?> response) {
        Gson gson = new Gson();
        Reader reader = response.errorBody().charStream();

        RestError.OauthError error;
        try {
            error = gson.fromJson(reader, RestError.OauthError.class);
        } catch (JsonParseException e) {
            error = new RestError.OauthError();
            error.error = e.getMessage();
        }
        return error;
    }

    /**
     * Parses the error body of Netatmo related REST responses.
     * <pre>
     *  {
     *     "error": {
     *         "code": int,
     *         "message": "string"     //optional
     *     }
     *  }
     * </pre>
     *
     * @param response REST response
     * @return an parsed error object
     */
    public static RestError.ApiError parseError(retrofit2.Response<?> response) {
        Gson gson = new Gson();
        Reader reader = response.errorBody().charStream();

        RestError.ApiError error;
        try {
            error = gson.fromJson(reader, RestError.ApiError.class);
        } catch (JsonParseException e) {
            error = new RestError.ApiError();
            error.error = new RestError.ApiError.Error();
            error.error.message = e.getMessage();
        }
        return error;
    }

}
