package com.shashank.platform.furnitureecommerceappui;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import losty.netatmo.NetatmoHttpClient;
import losty.netatmo.model.Measures;
import losty.netatmo.model.Module;
import losty.netatmo.model.Params;
import losty.netatmo.model.Station;

public class Main3Activity extends AppCompatActivity {

    private TextView module1Name;
    private TextView module2Name;
    private TextView livingTemperature;
    private TextView livingTimestamp;
    private TextView livingHumidity;
    private TextView sleepingTemperature;
    private TextView sleepingTimestamp;
    private TextView sleepingHumidity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        module1Name = findViewById(R.id.module1Name);
        module2Name = findViewById(R.id.module2Name);
        livingTemperature = findViewById(R.id.livingTemperature);
        livingTimestamp = findViewById(R.id.livingTimestamp);
        livingHumidity = findViewById(R.id.livingHumidity);

        sleepingTemperature = findViewById(R.id.sleepingTemperature);
        sleepingTimestamp = findViewById(R.id.sleepingTimestamp);
        sleepingHumidity = findViewById(R.id.sleepingHumidity);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        new Thread(new Runnable() {
            public void run() {
                // a potentially time consuming task
                getdata();
            }
        }).start();
    }

    private void getdata() {
        String clientId = getString(R.string.client_id);
        String clientSecret = getString(R.string.client_secret);

        NetatmoHttpClient client = new NetatmoHttpClient(clientId, clientSecret);
        String email = getString(R.string.email);
        String password = getString(R.string.password);
        client.login(email, password);

        List<Station> stationsData = client.getStationsData(null, null);
        Station station = stationsData.get(0);

        List<String> types = Arrays.asList(Params.TYPE_TEMPERATURE, Params.TYPE_HUMIDITY, Params.TYPE_CO2);


        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -15);
        Date minus15mins = calendar.getTime();

        List<DisplayInfo> displayInfos = new ArrayList<>();

        int moduleCounter = 1;
        for (Module module : station.getModules()) {
            List<Measures> measures = client.getMeasures(station, module, types, Params.SCALE_MAX, minus15mins, null, null, null);

            if (measures.size() == 0) {
                continue;
            }

            Measures measurement = measures.get(measures.size()-1);

            DisplayInfo displayInfo = new DisplayInfo();
            displayInfo.moduleName = module.getName();
            displayInfo.beginTime = measurement.getBeginTime();
            displayInfo.temperature = measurement.getTemperature();
            displayInfo.humidity = measurement.getHumidity();
            displayInfo.co2 = measurement.getCO2();

            showInfo(moduleCounter, displayInfo);
            moduleCounter++;
        }
    }

    private void showInfo(final int moduleCounter, final DisplayInfo displayInfo) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (moduleCounter == 1) {
                    module1Name.setText(displayInfo.moduleName);
                    livingTemperature.setText(String.valueOf(displayInfo.temperature));
                    livingTimestamp.setText("(" + displayInfo.getBeginTimeAsString() + " Uhr)");
                    livingHumidity.setText(String.valueOf(displayInfo.humidity));
                } else {
                    module2Name.setText(displayInfo.moduleName);
                    sleepingTemperature.setText(String.valueOf(displayInfo.temperature));
                    sleepingTimestamp.setText("(" + displayInfo.getBeginTimeAsString() + " Uhr)");
                    sleepingHumidity.setText(String.valueOf(displayInfo.humidity));
                }
            }
        });
    }

    private static class DisplayInfo {
        String moduleName;
        long beginTime;
        double temperature;
        double humidity;
        double co2;

        public String getBeginTimeAsString() {
            Date date = new Date(beginTime);
            DateFormat formatter = SimpleDateFormat.getTimeInstance(3);
            return formatter.format(date);
        }
    }
}
