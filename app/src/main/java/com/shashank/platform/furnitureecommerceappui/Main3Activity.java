package com.shashank.platform.furnitureecommerceappui;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
        String clientId = "";
        String clientSecret = "";

        NetatmoHttpClient client = new NetatmoHttpClient(clientId, clientSecret);
        String email = "";
        String password = "";
        client.login(email, password);

        List<Station> stationsData = client.getStationsData(null, null);
        Station station = stationsData.get(0);

        List<String> types = Arrays.asList(Params.TYPE_TEMPERATURE, Params.TYPE_HUMIDITY, Params.TYPE_CO2);


        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -15);
        Date minus15mins = calendar.getTime();

        for (Module module : station.getModules()) {
            List<Measures> measures = client.getMeasures(station, module, types, Params.SCALE_MAX, minus15mins, null, null, null);

            if (measures.size() == 0) {
                continue;
            }

            Measures measurement = measures.get(measures.size()-1);

            DisplayInfo module1 = new DisplayInfo();
            module1.moduleName = module.getName();
            module1.beginTime = measurement.getBeginTime();
            module1.temperature = measurement.getTemperature();
            module1.humidity = measurement.getHumidity();
            module1.co2 = measurement.getCO2();

            showInfo(module1);
        }
        System.out.println("Main.main()");
    }

    private void showInfo(final DisplayInfo module1) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                module1Name.setText(module1.moduleName);

                livingTemperature.setText(String.valueOf(module1.temperature));
                Date date = new Date(module1.beginTime);
                DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
                formatter.setTimeZone(TimeZone.getTimeZone("+2"));
                livingTimestamp.setText(formatter.format(date));
                livingHumidity.setText(String.valueOf(module1.humidity));
            }
        });
    }

    private static class DisplayInfo {
        String moduleName;
        long beginTime;
        double temperature;
        double humidity;
        double co2;
    }
}
