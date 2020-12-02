package com.github.kaiwinter.myatmo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.myatmo.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    static final String EXTRA_EMAIL = "EXTRA_EMAIL";
    static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";

    static final int RESULTCODE_LOGIN = 1;

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String error = extras.getString(MainActivity.EXTRA_LOGIN_ERROR);
            binding.error.setText(getString(R.string.login_login_error, error));

            String email = extras.getString(MainActivity.EXTRA_LOGIN_EMAIL);
            binding.username.setText(email);

            String password = extras.getString(MainActivity.EXTRA_LOGIN_PASSWORD);
            binding.password.setText(password);
        }

        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.username.getText().toString().length() == 0 && binding.password.getText().toString().length() == 0) {
                    binding.error.setText(R.string.login_empty);
                    return;
                }
                binding.login.setEnabled(false);
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(EXTRA_EMAIL, binding.username.getText().toString());
                intent.putExtra(EXTRA_PASSWORD, binding.password.getText().toString());
                setResult(RESULTCODE_LOGIN, intent);
                finish();
            }
        });
    }
}
