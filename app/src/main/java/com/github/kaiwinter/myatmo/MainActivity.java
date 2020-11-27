package com.github.kaiwinter.myatmo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.github.kaiwinter.myatmo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    static final String EXTRA_EMAIL = "EXTRA_EMAIL";
    static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String error = extras.getString(Main3Activity.EXTRA_LOGIN_ERROR);
            binding.error.setText(getString(R.string.login_login_error, error));
        }

        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Main3Activity.class);
                intent.putExtra(EXTRA_EMAIL, binding.username.getText().toString());
                intent.putExtra(EXTRA_PASSWORD, binding.password.getText().toString());
                setResult(0, intent);
                finish();
            }
        });
    }
}
