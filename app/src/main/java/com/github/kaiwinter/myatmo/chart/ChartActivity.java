package com.github.kaiwinter.myatmo.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.chart.rest.MeasureService;
import com.github.kaiwinter.myatmo.chart.rest.model.Measure;
import com.github.kaiwinter.myatmo.chart.rest.model.Measurement;
import com.github.kaiwinter.myatmo.databinding.ActivityChartBinding;
import com.github.kaiwinter.myatmo.login.AccessTokenManager;
import com.github.kaiwinter.myatmo.main.MeasurementType;
import com.github.kaiwinter.myatmo.rest.RestError;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;
import com.github.kaiwinter.myatmo.util.DateTimeUtil;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChartActivity extends AppCompatActivity {

    public static final String MODULE_ID = "MODULE_ID";
    public static final String DEVICE_ID = "DEVICE_ID";
    public static final String MEASUREMENT_TYPE = "MEASUREMENT_TYPE";
    public static final String MODULE_NAME = "MODULE_NAME";

    private ActivityChartBinding binding;
    private String moduleId;
    private String deviceId;
    private MeasurementType measurementType;

    private SharedPreferencesStore preferencesStore;
    private AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.chart.setNoDataText(getString(R.string.chart_no_data));

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
        }
        moduleId = extras.getString(MODULE_ID);
        deviceId = extras.getString(DEVICE_ID);
        String moduleName = extras.getString(MODULE_NAME);
        measurementType = (MeasurementType) extras.get(MEASUREMENT_TYPE);

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setTitle(moduleName);
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        preferencesStore = new SharedPreferencesStore(getApplicationContext());
        accessTokenManager = new AccessTokenManager(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getdata();
    }

    private void getdata() {
        if (TextUtils.isEmpty(preferencesStore.getAccessToken())) {
            finish(); // return to MainActivity for login
            return;
        }

        runOnUiThread(() -> binding.loadingIndicator.setVisibility(View.VISIBLE));

        if (accessTokenManager.accessTokenRefreshNeeded()) {
            accessTokenManager.refreshAccessToken(this::getdata, errormessage -> {
                Snackbar.make(binding.getRoot(), errormessage, Snackbar.LENGTH_LONG).show();
                runOnUiThread(() -> binding.loadingIndicator.setVisibility(View.INVISIBLE));
                finish(); // return to MainActivity for login
            });
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date startDate = calendar.getTime();

        ValueSupplier valueSupplier;
        if (measurementType == MeasurementType.TYPE_TEMPERATURE) {
            valueSupplier = new ValueSupplier.TemperatureValueSupplier();
        } else if (measurementType == MeasurementType.TYPE_HUMIDITY) {
            valueSupplier = new ValueSupplier.HumidityValueSupplier();
        } else if (measurementType == MeasurementType.TYPE_CO2) {
            valueSupplier = new ValueSupplier.CO2ValueSupplier();
        } else {
            finish();
            return;
        }

        MeasureService service = ServiceGenerator.createService(MeasureService.class);
        Call<Measure> call = service.getMeasure("Bearer " + preferencesStore.getAccessToken(), deviceId, moduleId, MeasurementType.SCALE_MAX.getApiString(), measurementType.getApiString(), (int) (startDate.getTime() / 1000), false);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Measure> call, Response<Measure> response) {
                if (!response.isSuccessful()) {
                    RestError.ApiError apiError = ServiceGenerator.parseError(response);
                    String detailMessage = apiError.error.message + " (" + apiError.error.code + ")";
                    Snackbar snackbar = Snackbar.make(binding.getRoot(), detailMessage, Snackbar.LENGTH_LONG);

                    if (response.code() == 401 || response.code() == 403) {
                        snackbar.setAction(R.string.logout_login, v -> {
                            preferencesStore.setTokens(null, null, -1);
                            finish();
                        });
                    }
                    snackbar.show();
                    runOnUiThread(() -> binding.loadingIndicator.setVisibility(View.INVISIBLE));
                    return;
                }
                Measure responseBody = response.body();

                List<Entry> entries = new ArrayList<>();
                List<Measurement> measurements = responseBody.body.measurements;
                for (Measurement measurement : measurements) {
                    entries.add(new Entry(measurement.beginTime, (float) measurement.value[0]));
                }

                LineDataSet dataSet = new LineDataSet(entries, getString(valueSupplier.getLabel()) + getString(R.string.chart_timespan));
                dataSet.setDrawFilled(true);
                int color = hex2RGB(R.color.colorPrimaryDark);

                dataSet.setFillColor(color);
                dataSet.setColors(color);
                dataSet.setDrawCircles(false);

                LineData lineData = new LineData(dataSet);
                lineData.setDrawValues(false);

                binding.chart.setData(lineData);

                XAxis xAxis = binding.chart.getXAxis();
                xAxis.setValueFormatter(new ValueFormatter() {

                    @Override
                    public String getFormattedValue(float value) {
                        return DateTimeUtil.getDateAsShortTimeString((long) value);
                    }
                });

                // create marker to display box when values are selected
                MarkerView markerView = new MyMarkerView(ChartActivity.this, R.layout.custom_marker_view, valueSupplier.formatStringId());
                markerView.setChartView(binding.chart);
                binding.chart.setMarker(markerView);

                binding.chart.getDescription().setEnabled(false);

                runOnUiThread(() -> {
                    binding.chart.invalidate();
                    binding.loadingIndicator.setVisibility(View.INVISIBLE);
                });
            }

            @Override
            public void onFailure(Call<Measure> call, Throwable t) {
                runOnUiThread(() -> {
                    String message = getString(R.string.netatmo_connection_error, t.getMessage());
                    Snackbar.make(binding.loadingIndicator, message, Snackbar.LENGTH_LONG).show();
                    binding.loadingIndicator.setVisibility(View.INVISIBLE);
                });
            }
        });

    }

    private int hex2RGB(int colorId) {
        int color = ContextCompat.getColor(getApplicationContext(), colorId);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.rgb(r, g, b);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
