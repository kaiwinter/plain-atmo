package com.github.kaiwinter.myatmo.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.chart.ChartActivity;
import com.github.kaiwinter.myatmo.databinding.ActivityMainBinding;
import com.github.kaiwinter.myatmo.login.LoginActivity;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements ViewModelStoreOwner {

    private ActivityMainBinding binding;
    private MainActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this, new MainActivityViewModelFactory(getApplication())).get(MainActivityViewModel.class);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.setViewmodel(viewModel);

        viewModel.userMessage.observe(this, message -> Snackbar.make(binding.loadingIndicator, message.getMessage(this), Snackbar.LENGTH_LONG).show());

        viewModel.navigateToLoginActivity.observe(this, __ -> startLoginActivity());

        viewModel.navigateToRelogin.observe(this, message -> {
            Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.logout_login, v -> {
                viewModel.clearTokens();
                startLoginActivity();
            });
            snackbar.show();
        });

        viewModel.navigateToChartActivity.observe(this, switchToChartActivityVO -> {
            if (switchToChartActivityVO.getStationType() == StationType.INDOOR_STATION) {
                showIndoorChart(switchToChartActivityVO.getMeasurementType());
            } else if (switchToChartActivityVO.getStationType() == StationType.OUTDOOR_STATION) {
                showOutdoorChart(switchToChartActivityVO.getMeasurementType());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.getdata();
    }

    private void showIndoorChart(MeasurementType measurementType) {
        if (viewModel.indoorModule.getValue() == null) {
            return;
        }

        Intent intent = new Intent(getApplicationContext(), ChartActivity.class);
        intent.putExtra(ChartActivity.DEVICE_ID, viewModel.indoorModule.getValue().id);
        intent.putExtra(ChartActivity.MODULE_NAME, viewModel.indoorModule.getValue().moduleName);
        intent.putExtra(ChartActivity.MEASUREMENT_TYPE, measurementType);
        startActivity(intent);
    }

    private void showOutdoorChart(MeasurementType measurementType) {
        if (viewModel.indoorModule.getValue() == null || viewModel.outdoorModule.getValue() == null) {
            return;
        }

        Intent intent = new Intent(getApplicationContext(), ChartActivity.class);
        intent.putExtra(ChartActivity.DEVICE_ID, viewModel.indoorModule.getValue().id);
        intent.putExtra(ChartActivity.MODULE_ID, viewModel.outdoorModule.getValue().id);
        intent.putExtra(ChartActivity.MODULE_NAME, viewModel.outdoorModule.getValue().moduleName);
        intent.putExtra(ChartActivity.MEASUREMENT_TYPE, measurementType);
        startActivity(intent);
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        // Make the app exit if back is pressed on main activity. Else the user returns to the Login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
