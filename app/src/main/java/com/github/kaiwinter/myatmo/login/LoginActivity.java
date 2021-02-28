package com.github.kaiwinter.myatmo.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.databinding.ActivityLoginBinding;
import com.github.kaiwinter.myatmo.login.rest.LoginService;
import com.github.kaiwinter.myatmo.main.MainActivity;
import com.github.kaiwinter.myatmo.login.rest.model.AccessToken;
import com.github.kaiwinter.myatmo.rest.NetatmoCallback;
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
            changeLoadingIndicatorVisibility(View.VISIBLE);
            binding.login.setEnabled(false);
            String code = data.getQueryParameter("code");
            LoginService service = ServiceGenerator.createService(LoginService.class);

            String clientId = getString(R.string.client_id);
            String clientSecret = getString(R.string.client_secret);

            Call<AccessToken> call = service.getAccessToken(clientId, clientSecret, redirectUri, "authorization_code", code, "read_station");
            call.enqueue(new NetatmoCallback<AccessToken>(this, binding.loadingIndicator) {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                    AccessToken body = response.body();
                    if (response.code() == 200) {
                        SharedPreferencesTokenStore tokenstore = new SharedPreferencesTokenStore(LoginActivity.this);
                        long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000;
                        tokenstore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        LoginActivity.this.startActivity(intent);

                    } else {
                        binding.error.setText(getString(R.string.login_login_error, "HTTP code " + response.code()));
                    }
                    changeLoadingIndicatorVisibility(View.INVISIBLE);
                    binding.login.setEnabled(true);
                }
            });

        } else if (data.getQueryParameterNames().contains("error")) {
            String error = data.getQueryParameter("error");
            binding.error.setText(getString(R.string.login_login_error, error));
            changeLoadingIndicatorVisibility(View.INVISIBLE);
            binding.login.setEnabled(true);
        }

    }

    private void changeLoadingIndicatorVisibility(final int visibility) {
        if (onUiThread()) {
            binding.loadingIndicator.setVisibility(visibility);
        } else {
            runOnUiThread(() -> binding.loadingIndicator.setVisibility(visibility));
        }
    }

    private boolean onUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
