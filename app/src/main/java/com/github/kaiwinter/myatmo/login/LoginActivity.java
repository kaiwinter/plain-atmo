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
import com.github.kaiwinter.myatmo.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    public static final String REDIRERECT_URI = "com.github.kaiwinter.myatmo://login";

    private ActivityLoginBinding binding;
    private AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        accessTokenManager = new AccessTokenManager(getApplicationContext());
    }

    /**
     * Called from the login button defined in the XML.
     */
    public void loginClicked(View view) {
        String clientId = getString(R.string.client_id);
        String url = "https://api.netatmo.com/oauth2/authorize" + "?client_id=" + clientId + "&redirect_uri=" + REDIRERECT_URI + "&scope=read_station";

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
            accessTokenManager.retrieveAccessToken(getApplication(), code, () -> {
                // finish(); // return to MainActivity
                startMainActivity(); // calling finish() doesn't work if firefox was used for OAUTH flow.
                hideLoadingState(null);
            }, errormessage -> {
                binding.error.setText(errormessage);
                hideLoadingState(null);
            });

        } else if (data.getQueryParameterNames().contains("error")) {
            String error = data.getQueryParameter("error");
            hideLoadingState(error);
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        // Make the app exit if back is pressed on login activity. Else the user is trapped there
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
