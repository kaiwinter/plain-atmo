package com.github.kaiwinter.myatmo.login;

import android.content.Context;
import android.text.TextUtils;

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

    private final Context context;
    private final SharedPreferencesStore preferencesStore;

    public AccessTokenManager(Context context) {
        this.context = context;
        this.preferencesStore = new SharedPreferencesStore(context);
    }

    /**
     * Checks if the stored access token is expired.
     *
     * @return true if the access token is expired else false
     */
    public boolean accessTokenRefreshNeeded() {
        long expiresAt = preferencesStore.getExpiresAt();
        long currentTimestamp = System.currentTimeMillis();

        return currentTimestamp + (EXPIRE_TOLERANCE_SECONDS * 1000) >= expiresAt;
    }

    /**
     * Retrieves the access token by a previously acquired code.
     *
     * @param code      the previously acquired code
     * @param onSuccess Runnable which is called on success
     * @param onError   Consumer which is called on an error, receives the error message
     */
    public void retrieveAccessToken(String code, Runnable onSuccess, Consumer<String> onError) {
        LoginService service = ServiceGenerator.createService(LoginService.class);

        String clientId = context.getString(R.string.client_id);
        String clientSecret = context.getString(R.string.client_secret);

        Call<AccessToken> call = service.getAccessToken(clientId, clientSecret, LoginActivity.REDIRERECT_URI, "authorization_code", code, "read_station");
        call.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                if (response.isSuccessful()) {
                    SharedPreferencesStore preferencesStore = new SharedPreferencesStore(context);
                    AccessToken body = response.body();
                    long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000;
                    preferencesStore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                    onSuccess.run();

                } else {
                    APIError apiError = ServiceGenerator.parseError(response);
                    // Print HTTP error code and message and code from API Response
                    onError.accept(context.getString(R.string.login_login_error, apiError.error.message + " (" + response.code() + ", " + apiError.error.code + ")"));
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                String errormessage = context.getString(R.string.netatmo_connection_error, t.getMessage());
                onError.accept(errormessage);
            }
        });
    }

    /**
     * Refreshes the access token.
     *
     * @param onSuccess Runnable which is called on success
     * @param onError   Consumer which is called on an error, receives the error message
     */
    public void refreshAccessToken(Runnable onSuccess, Consumer<String> onError) {
        LoginService loginService = ServiceGenerator.createService(LoginService.class);

        String clientId = context.getString(R.string.client_id);
        String clientSecret = context.getString(R.string.client_secret);
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(clientSecret)) {
            onError.accept(context.getString(R.string.missing_client_configuration));
            return;
        }
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
                String errormessage = context.getString(R.string.netatmo_connection_error, t.getMessage());
                onError.accept(errormessage);
            }
        });
    }
}
