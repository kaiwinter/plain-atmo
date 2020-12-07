package com.github.kaiwinter.myatmo.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.myatmo.main.MainActivity;
import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_ERROR = "EXTRA_ERROR";
    public static final String EXTRA_EMAIL = "EXTRA_EMAIL";
    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";

    public static final int RESULTCODE_LOGIN = 1;

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Makes activity scrollable if keyboard is shown
        AndroidBug5497Workaround.assistActivity(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String error = extras.getString(EXTRA_ERROR);
            binding.error.setText(getString(R.string.login_login_error, error));

            String email = extras.getString(EXTRA_EMAIL);
            binding.email.setText(email);

            String password = extras.getString(EXTRA_PASSWORD);
            binding.password.setText(password);
        }
    }

    /**
     * Called from the login button defined in the XML.
     */
    public void loginClicked(View view) {
        if (binding.email.getText().toString().length() == 0 || binding.password.getText().toString().length() == 0) {
            binding.error.setText(R.string.login_empty);
            return;
        }
        binding.login.setEnabled(false);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(EXTRA_EMAIL, binding.email.getText().toString());
        intent.putExtra(EXTRA_PASSWORD, binding.password.getText().toString());
        setResult(RESULTCODE_LOGIN, intent);
        finish();
    }
}
