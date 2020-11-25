package com.github.kaiwinter.myatmo;

import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private TextView livingCo2;
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
        livingCo2 = findViewById(R.id.livingCo2);

        sleepingTemperature = findViewById(R.id.sleepingTemperature);
        sleepingTimestamp = findViewById(R.id.sleepingTimestamp);
        sleepingHumidity = findViewById(R.id.sleepingHumidity);

        FloatingActionButton fab = findViewById(R.id.refreshButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResume();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Thread(new Runnable() {
            public void run() {
                getdata();
            }
        }).start();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private boolean isOffline() {
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress("api.netatmo.net", 443), 1500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void getdata() {
        if (isOffline()) {
            Snackbar.make(Main3Activity.this.findViewById(R.id.main), "Keine Internetverbindung", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }

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

            if (module.getType().equals(Module.TYPE_INDOOR)) {
                displayInfo.co2 = measurement.getCO2();
                displayInfo.moduleType = DisplayInfo.ModuleType.INDOOR;
            } else if (module.getType().equals(Module.TYPE_OUTDOOR)) {
                displayInfo.moduleType = DisplayInfo.ModuleType.OUTDOOR;
            } else {
                throw new IllegalArgumentException("Not supported module type: " + module.getType());
            }

            showInfo(displayInfo);
        }
    }

    private void showInfo(final DisplayInfo displayInfo) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (displayInfo.moduleType == DisplayInfo.ModuleType.INDOOR) {
                    module1Name.setText(displayInfo.moduleName);
                    livingTimestamp.setText(getString(R.string.display_timestamp, displayInfo.getBeginTimeAsString()));
                    livingTemperature.setText(getString(R.string.display_temperature, displayInfo.temperature));
                    livingHumidity.setText(getString(R.string.display_humidity, displayInfo.humidity));
                    livingCo2.setText(getString(R.string.display_co2, displayInfo.co2));
                } else if (displayInfo.moduleType == DisplayInfo.ModuleType.OUTDOOR) {
                    module2Name.setText(displayInfo.moduleName);
                    sleepingTimestamp.setText(getString(R.string.display_timestamp, displayInfo.getBeginTimeAsString()));
                    sleepingTemperature.setText(getString(R.string.display_temperature, displayInfo.temperature));
                    sleepingHumidity.setText(getString(R.string.display_humidity, displayInfo.humidity));
                } else {
                    throw new IllegalArgumentException("Not supported module type: " + displayInfo.moduleType);
                }
            }
        });
    }

    private static class DisplayInfo {
        enum ModuleType {
            INDOOR, OUTDOOR
        }

        String moduleName;
        ModuleType moduleType;
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
