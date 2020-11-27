package com.github.kaiwinter.myatmo;

import android.content.Intent;
import android.os.Looper;

import com.github.kaiwinter.myatmo.databinding.ActivityMain3Binding;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import losty.netatmo.NetatmoHttpClient;
import losty.netatmo.exceptions.NetatmoOAuthException;
import losty.netatmo.model.Measures;
import losty.netatmo.model.Module;
import losty.netatmo.model.Params;
import losty.netatmo.model.Station;

public class Main3Activity extends AppCompatActivity {

    public static final int REQUEST_CODE_LOGIN = 1;
    public static final int REQUEST_CODE_LOGIN_ONERROR = 2;

    private ActivityMain3Binding binding;
    private NetatmoHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        binding = ActivityMain3Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String clientId = getString(R.string.client_id);
        String clientSecret = getString(R.string.client_secret);

        SharedPreferencesTokenStore tokenstore = new SharedPreferencesTokenStore(this);
        tokenstore.setTokens(null, null, -1);
        client = new NetatmoHttpClient(clientId, clientSecret, tokenstore);

        binding.refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getdata();
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

    private boolean isOffline() {
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress("api.netatmo.net", 443), 1500);
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private void getdata() {
        if (client.getOAuthStatus() == NetatmoHttpClient.OAuthStatus.NO_LOGIN) {
            startLoginActivity();
            return;
        }
        if (isOffline()) {
            Snackbar.make(Main3Activity.this.findViewById(R.id.main), "Keine Internetverbindung", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            changeLoadingIndicatorVisibility(View.INVISIBLE);
            return;
        }
        changeLoadingIndicatorVisibility(View.VISIBLE);

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
                // ignore, maybe extend later
            }

            showInfo(displayInfo);
        }
        changeLoadingIndicatorVisibility(View.INVISIBLE);
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(intent, REQUEST_CODE_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final String email = data.getStringExtra("email");
        final String password = data.getStringExtra("password");
        if (email != null || password != null) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        client.login(email, password);
                    } catch (NetatmoOAuthException e) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("error", e.getMessage());
                        startActivityForResult(intent, REQUEST_CODE_LOGIN_ONERROR);
                    }
                }
            }).start();
        }
    }

    private void changeLoadingIndicatorVisibility(final int visibility) {
        if (onUiThread()) {
            binding.loadingIndicator.setVisibility(visibility);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.loadingIndicator.setVisibility(visibility);
                }
            });
        }
    }

    private boolean onUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private void showInfo(final DisplayInfo displayInfo) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (displayInfo.moduleType == DisplayInfo.ModuleType.INDOOR) {
                    binding.module1Name.setText(displayInfo.moduleName);
                    binding.livingTimestamp.setText(getString(R.string.display_timestamp, displayInfo.getBeginTimeAsString()));
                    binding.livingTemperature.setText(getString(R.string.display_temperature, displayInfo.temperature));
                    binding.livingHumidity.setText(getString(R.string.display_humidity, displayInfo.humidity));
                    binding.livingCo2.setText(getString(R.string.display_co2, displayInfo.co2));
                } else if (displayInfo.moduleType == DisplayInfo.ModuleType.OUTDOOR) {
                    binding.module2Name.setText(displayInfo.moduleName);
                    binding.sleepingTimestamp.setText(getString(R.string.display_timestamp, displayInfo.getBeginTimeAsString()));
                    binding.sleepingTemperature.setText(getString(R.string.display_temperature, displayInfo.temperature));
                    binding.sleepingHumidity.setText(getString(R.string.display_humidity, displayInfo.humidity));
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
