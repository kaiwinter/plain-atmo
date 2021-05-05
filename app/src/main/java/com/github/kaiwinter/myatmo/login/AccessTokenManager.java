package com.github.kaiwinter.myatmo.login;

import android.content.Context;

import androidx.core.util.Consumer;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.login.rest.LoginService;
import com.github.kaiwinter.myatmo.login.rest.model.AccessToken;
import com.github.kaiwinter.myatmo.rest.APIError;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccessTokenManager {
    private static final long EXPIRE_TOLERANCE_SECONDS = 60;

    private final SharedPreferencesStore preferencesStore;

    public AccessTokenManager(Context context) {
        preferencesStore = new SharedPreferencesStore(context);
    }

    public boolean accessTokenRefreshNeeded() {
        long expiresAt = preferencesStore.getExpiresAt();
        long currentTimestamp = System.currentTimeMillis();

        return currentTimestamp + (EXPIRE_TOLERANCE_SECONDS * 1000) >= expiresAt;
    }

    public void refreshAccessToken(Context context, Runnable onSuccess, Consumer<String> onError) {
        LoginService loginService = ServiceGenerator.createService(LoginService.class);

        String clientId = context.getString(R.string.client_id);
        String clientSecret = context.getString(R.string.client_secret);
        String refreshToken = preferencesStore.getRefreshToken();
        Call<AccessToken> call = loginService.refreshToken(clientId, clientSecret, "refresh_token", refreshToken);
        call.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    AccessToken body = response.body();
                    long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000;
                    preferencesStore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                    onSuccess.run();
                } else {
                    APIError apiError = ServiceGenerator.parseError(response);
                    String errormessage = apiError.error.message + " (" + response.code() + ", " + apiError.error.code + ")";
                    onError.accept(errormessage);
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                String errormessage = context.getString(R.string.netatmo_connection_error);
                onError.accept(errormessage);
            }
        });
    }
}
