package com.github.kaiwinter.myatmo.login.rest;

import com.github.kaiwinter.myatmo.login.rest.model.AccessToken;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface LoginService {
    @FormUrlEncoded
    @POST("/oauth2/token")
    Call<AccessToken> getAccessToken(@Field("client_id") String clientId,
                                     @Field("client_secret") String clientSecret,
                                     @Field("redirect_uri") String redirectUri,
                                     @Field("grant_type") String grantType,
                                     @Field("code") String code,
                                     @Field("scope") String scope);

    @FormUrlEncoded
    @POST("/oauth2/token")
    Call<AccessToken> refreshToken(@Field("client_id") String clientId,
                                   @Field("client_secret") String clientSecret,
                                   @Field("grant_type") String grantType,
                                   @Field("refresh_token") String refreshToken);
}