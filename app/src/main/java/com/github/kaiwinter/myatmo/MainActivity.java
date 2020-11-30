package com.github.kaiwinter.myatmo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.myatmo.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import losty.netatmo.NetatmoHttpClient;
import losty.netatmo.exceptions.NetatmoNotLoggedInException;
import losty.netatmo.exceptions.NetatmoOAuthException;
import losty.netatmo.exceptions.NetatmoParseException;
import losty.netatmo.model.Measures;
import losty.netatmo.model.Module;
import losty.netatmo.model.Params;
import losty.netatmo.model.Station;

public class MainActivity extends AppCompatActivity {

    static final String EXTRA_LOGIN_ERROR = "EXTRA_LOGIN_ERROR";
    private final AtomicBoolean inLoginProcess = new AtomicBoolean(false);
    private ActivityMainBinding binding;
    private NetatmoHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String clientId = getString(R.string.client_id);
        String clientSecret = getString(R.string.client_secret);

        SharedPreferencesTokenStore tokenstore = new SharedPreferencesTokenStore(this);
        //tokenstore.setTokens(null, null, -1);
        client = new NetatmoHttpClient(clientId, clientSecret, tokenstore);

        binding.refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    public void run() {
                        getdata();
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inLoginProcess.get()) {
            return;
        }
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

    /**
     * Calls {@link #getdata_internal()} and handles thrown exceptions. If moving away from lost-carrier:netatmo-api this can be improved.
     */
    private void getdata() {
        try {
            changeLoadingIndicatorVisibility(View.VISIBLE);
            getdata_internal();
        } catch (NetatmoNotLoggedInException |NetatmoOAuthException | NetatmoParseException e) {
            Snackbar.make(MainActivity.this.findViewById(R.id.main), getString(R.string.error_loading_data, unwrapException(e)), Snackbar.LENGTH_LONG).show();
        } finally {
            changeLoadingIndicatorVisibility(View.INVISIBLE);
        }
    }

    private void getdata_internal() {
        if (client.getOAuthStatus() == NetatmoHttpClient.OAuthStatus.NO_LOGIN) {
            startLoginActivity();
            return;
        }
        if (isOffline()) {
            Snackbar.make(MainActivity.this.findViewById(R.id.main), R.string.no_connection, Snackbar.LENGTH_LONG).show();
            return;
        }

        List<Station> stationsData = client.getStationsData(null, null);
        Station station = stationsData.get(0);

        List<String> types = Arrays.asList(Params.TYPE_TEMPERATURE, Params.TYPE_HUMIDITY, Params.TYPE_CO2);

        for (Module module : station.getModules()) {
            Measures measurement = client.getLastMeasurement(station, module, types, Params.SCALE_MAX);

            if (measurement == null) {
                continue;
            }

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
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivityForResult(intent, 0);
    }

    /**
     * Called from a Runnable in onActivityResult() that calls the REST service to log-in.
     * Meanwhile onResume() is called which triggers a startLoginActivity(), so the "inLoginProcess"-check is necessary only there.
     *
     * @param errorMessage Error message to show the user on the login screen
     */
    private void startLoginActivityWithErrorMessage(String errorMessage) {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.putExtra(EXTRA_LOGIN_ERROR, errorMessage);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != LoginActivity.RESULTCODE_LOGIN) {
            // back pressed on login screen -> exit app
            finish();
            return;
        }
        final String email = data.getStringExtra(LoginActivity.EXTRA_EMAIL);
        final String password = data.getStringExtra(LoginActivity.EXTRA_PASSWORD);
        if (email == null || password == null) {
            return;
        }
        if (email.length() == 0 || password.length() == 0) {
            return;
        }
        inLoginProcess.set(true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    client.login(email, password);
                } catch (NetatmoOAuthException e) {
                    String error = unwrapException(e);
                    startLoginActivityWithErrorMessage(error);
                } finally {
                    inLoginProcess.set(false);
                    getdata();
                }
            }
        }).start();
    }

    private String unwrapException(Throwable e) {
        Throwable cause = e.getCause();
        if (cause instanceof OAuthProblemException) {
            return ((OAuthProblemException) e.getCause()).getError();
        } else if (cause instanceof OAuthSystemException) {
            return ((OAuthSystemException) e.getCause()).getMessage();
        }
        return e.getMessage();
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

        enum ModuleType {
            INDOOR, OUTDOOR
        }
    }
}
