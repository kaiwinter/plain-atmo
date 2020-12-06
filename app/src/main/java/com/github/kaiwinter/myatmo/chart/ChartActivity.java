package com.github.kaiwinter.myatmo.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.databinding.ActivityChartBinding;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesTokenStore;
import com.github.kaiwinter.myatmo.util.DateTimeUtil;
import com.github.kaiwinter.myatmo.util.ExceptionUtil;
import com.github.kaiwinter.myatmo.util.NetworkUtil;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import losty.netatmo.NetatmoHttpClient;
import losty.netatmo.exceptions.NetatmoNotLoggedInException;
import losty.netatmo.exceptions.NetatmoOAuthException;
import losty.netatmo.exceptions.NetatmoParseException;
import losty.netatmo.model.Measures;
import losty.netatmo.model.Module;
import losty.netatmo.model.Params;
import losty.netatmo.model.Station;

public class ChartActivity extends AppCompatActivity {

    public static final String STATION_ID = "STATION_ID";
    public static final String MODULE_ID = "MODULE_ID";
    public static final String MEASUREMENT_TYPE = "MEASUREMENT_TYPE";
    public static final String MODULE_NAME = "MODULE_NAME";

    private NetatmoHttpClient client;
    private ActivityChartBinding binding;
    private String stationId;
    private String moduleId;
    private String measurementType;

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
        stationId = extras.getString(STATION_ID);
        moduleId = extras.getString(MODULE_ID);
        String moduleName = extras.getString(MODULE_NAME);
        measurementType = extras.getString(MEASUREMENT_TYPE);

        String clientId = getString(R.string.client_id);
        String clientSecret = getString(R.string.client_secret);

        SharedPreferencesTokenStore tokenstore = new SharedPreferencesTokenStore(this);
        client = new NetatmoHttpClient(clientId, clientSecret, tokenstore);

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setTitle(moduleName);
        supportActionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(this::getdata).start();
    }

    /**
     * Calls {@link #getdata_internal()} and handles thrown exceptions. If moving away from lost-carrier:netatmo-api this can be improved.
     */
    private void getdata() {
        try {
            changeLoadingIndicatorVisibility(View.VISIBLE);
            getdata_internal();
        } catch (NetatmoNotLoggedInException | NetatmoOAuthException | NetatmoParseException e) {
            Log.e("myatmo", e.getMessage(), e);
            Snackbar.make(binding.getRoot(), getString(R.string.error_loading_data, ExceptionUtil.unwrapException(e)), Snackbar.LENGTH_LONG).show();
        } finally {
            changeLoadingIndicatorVisibility(View.INVISIBLE);
        }
    }

    private void getdata_internal() {
        if (client.getOAuthStatus() == NetatmoHttpClient.OAuthStatus.NO_LOGIN) {
            finish(); // return to MainActivity for login
            return;
        }
        if (NetworkUtil.isOffline()) {
            Snackbar.make(ChartActivity.this.findViewById(R.id.main), R.string.no_connection, Snackbar.LENGTH_LONG).show();
            return;
        }

        Station station = new Station(null, stationId);
        Module module = new Module(null, moduleId, null);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date startDate = calendar.getTime();

        List<Measures> measures = client.getMeasures(station, module, Collections.singletonList(measurementType), Params.SCALE_MAX, startDate, null, null, null);

        ValueSupplier valueSupplier;
        if (Params.TYPE_TEMPERATURE.equals(measurementType)) {
            valueSupplier = new ValueSupplier.TemperatureValueSupplier();
        } else if (Params.TYPE_HUMIDITY.equals(measurementType)) {
            valueSupplier = new ValueSupplier.HumidityValueSupplier();
        } else if (Params.TYPE_CO2.equals(measurementType)) {
            valueSupplier = new ValueSupplier.CO2ValueSupplier();
        } else {
            finish();
            return;
        }

        // Create chart
        List<Entry> entries = new ArrayList<>();
        for (Measures measurement : measures) {
            entries.add(new Entry(measurement.getBeginTime(), valueSupplier.getValue(measurement)));
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
        MarkerView markerView = new MyMarkerView(this, R.layout.custom_marker_view, valueSupplier.formatStringId());
        markerView.setChartView(binding.chart);
        binding.chart.setMarker(markerView);

        binding.chart.getDescription().setEnabled(false);
        runOnUiThread(() -> binding.chart.invalidate());
    }

    private int hex2RGB(int colorId) {
        int color = getResources().getColor(colorId);
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
