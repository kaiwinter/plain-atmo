package com.github.kaiwinter.myatmo.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.databinding.ActivityLoginBinding;
import com.github.kaiwinter.myatmo.login.rest.LoginService;
import com.github.kaiwinter.myatmo.login.rest.model.AccessToken;
import com.github.kaiwinter.myatmo.rest.APIError;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesTokenStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private final String redirectUri = "auth://callback";

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /**
     * Called from the login button defined in the XML.
     */
    public void loginClicked(View view) {
        String clientId = getString(R.string.client_id);
        String url = "https://api.netatmo.com/oauth2/authorize" + "?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&scope=read_station";

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        Uri data = intent.getData();
        if (data == null) {
            return;
        }
        if (data.getQueryParameterNames().contains("code")) {
            showLoadingState();

            String code = data.getQueryParameter("code");
            LoginService service = ServiceGenerator.createService(LoginService.class);

            String clientId = getString(R.string.client_id);
            String clientSecret = getString(R.string.client_secret);

            Call<AccessToken> call = service.getAccessToken(clientId, clientSecret, redirectUri, "authorization_code", code, "read_station");
            call.enqueue(new Callback<AccessToken>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                    if (response.code() == 200) {
                        SharedPreferencesTokenStore tokenstore = new SharedPreferencesTokenStore(LoginActivity.this);
                        AccessToken body = response.body();
                        long expiresAt = System.currentTimeMillis() / 1000 + body.expiresIn;
                        tokenstore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                        finish(); // return to MainActivity
                    } else {
                        APIError apiError = ServiceGenerator.parseError(response);
                        // Print HTTP error code and message and code from API Response
                        binding.error.setText(getString(R.string.login_login_error, apiError.error.message + " (" + response.code() + ", " + apiError.error.code + ")"));
                    }
                    hideLoadingState(null);
                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    hideLoadingState(getString(R.string.login_login_error, t.getMessage()));
                }
            });

        } else if (data.getQueryParameterNames().contains("error")) {
            String error = data.getQueryParameter("error");
            hideLoadingState(getString(R.string.login_login_error, error));
        }
    }

    private void showLoadingState() {
        runOnUiThread(() -> {
            binding.loadingIndicator.setVisibility(View.VISIBLE);
            binding.login.setEnabled(false);
        });
    }

    private void hideLoadingState(String errormessage) {
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(errormessage)) {
                binding.error.setText(getString(R.string.login_login_error, errormessage));
            }
            binding.loadingIndicator.setVisibility(View.INVISIBLE);
            binding.login.setEnabled(true);
        });
    }
}
