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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ViewModelStoreOwner {

    private ActivityMainBinding binding;
    private MainActivityViewModel viewModel;

    private List<Snackbar> snackbarQueue = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this, new MainActivityViewModelFactory(getApplication())).get(MainActivityViewModel.class);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.setViewmodel(viewModel);

        prepareLineChart(binding.module1TemperatureChart);
        prepareLineChart(binding.module1HumidityChart);
        prepareLineChart(binding.module1Co2Chart);
        prepareLineChart(binding.module1NoiseChart);
        prepareLineChart(binding.module2TemperatureChart);
        prepareLineChart(binding.module2HumidityChart);

        viewModel.userMessage.observe(this, message -> Snackbar.make(binding.loadingIndicator, message.getMessage(this), Snackbar.LENGTH_LONG).show());

        viewModel.navigateToLoginActivity.observe(this, __ -> startLoginActivity());

        viewModel.navigateToRelogin.observe(this, message -> {
            Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.logout_login, v -> {
                viewModel.clearTokens();
                startLoginActivity();
            });
            addToSnackbarQueue(snackbar);
        });

        viewModel.navigateToChartActivity.observe(this, switchToChartActivityVO -> {
            if (switchToChartActivityVO.getStationType() == StationType.INDOOR_STATION) {
                showIndoorChart(switchToChartActivityVO.getMeasurementType());
            } else if (switchToChartActivityVO.getStationType() == StationType.OUTDOOR_STATION) {
                showOutdoorChart(switchToChartActivityVO.getMeasurementType());
            }
        });

        viewModel.indoorModuleTemperatureChartValues.observe(this, entries -> showLineData(entries, binding.module1TemperatureChart));
        viewModel.indoorModuleHumidityChartValues.observe(this, entries -> showLineData(entries, binding.module1HumidityChart));
        viewModel.indoorModuleCo2ChartValues.observe(this, entries -> showLineData(entries, binding.module1Co2Chart));
        viewModel.indoorModuleNoiseChartValues.observe(this, entries -> showLineData(entries, binding.module1NoiseChart));
        viewModel.outdoorModuleTemperatureChartValues.observe(this, entries -> showLineData(entries, binding.module2TemperatureChart));
        viewModel.outdoorModuleHumidityChartValues.observe(this, entries -> showLineData(entries, binding.module2HumidityChart));
    }

    private void addToSnackbarQueue(Snackbar snackbar) {
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                snackbarQueue.remove(snackbar);
                if (!snackbarQueue.isEmpty()) {
                    snackbarQueue.get(0).show();
                }
            }
        });
        snackbarQueue.add(snackbar);
        if (snackbarQueue.size() == 1) {
            snackbar.show();
        }
    }

    /**
     * Setup the mini charts which are shown on the cards.
     */
    private void prepareLineChart(LineChart lineChart) {
        lineChart.setNoDataText("");
        lineChart.getDescription().setEnabled(false);

        lineChart.getXAxis().setEnabled(false);

        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawAxisLine(false);

        lineChart.getAxisRight().setDrawGridLines(false);
        lineChart.getAxisRight().setDrawAxisLine(false);

        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getLegend().setEnabled(false);

        lineChart.setTouchEnabled(false);
        lineChart.setViewPortOffsets(0f, 0f, 0f, 0f);
    }

    /**
     * Show loaded data in the mini charts on the cards.
     */
    private void showLineData(List<Entry> entries, LineChart lineChart) {
        LineDataSet dataSet = new LineDataSet(entries, null);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        runOnUiThread(lineChart::invalidate);
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
