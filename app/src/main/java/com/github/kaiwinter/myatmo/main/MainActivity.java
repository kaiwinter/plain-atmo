package com.github.kaiwinter.myatmo.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.databinding.ActivityMainBinding;
import com.github.kaiwinter.myatmo.login.LoginActivity;
import com.google.android.material.snackbar.Snackbar;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
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

    private static final List<String> NETATMO_TYPES = Arrays.asList(Params.TYPE_TEMPERATURE, Params.TYPE_HUMIDITY, Params.TYPE_CO2);
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
    }

    /**
     * Called from the refresh button defined in the XML.
     */
    public void refreshButtonClicked(View view) {
        new Thread(this::getdata).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inLoginProcess.get()) {
            return;
        }
        new Thread(this::getdata).start();
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
        } catch (NetatmoNotLoggedInException | NetatmoOAuthException | NetatmoParseException e) {
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

        for (Module module : station.getModules()) {
            Measures measurement = client.getLastMeasurement(station, module, NETATMO_TYPES);

            if (measurement == null) {
                continue;
            }

            ModuleVO moduleVO = new ModuleVO();
            moduleVO.moduleName = module.getName();
            moduleVO.beginTime = measurement.getBeginTime();
            moduleVO.temperature = measurement.getTemperature();
            moduleVO.humidity = measurement.getHumidity();

            if (module.getType().equals(Module.TYPE_INDOOR)) {
                moduleVO.co2 = measurement.getCO2();
                moduleVO.moduleType = ModuleVO.ModuleType.INDOOR;
            } else if (module.getType().equals(Module.TYPE_OUTDOOR)) {
                moduleVO.moduleType = ModuleVO.ModuleType.OUTDOOR;
            } else {
                // ignore, maybe extend later
            }

            showInfo(moduleVO);
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
     * @param email        the email which was entered previously
     * @param password     the password which was entered previously
     */
    private void startLoginActivityWithErrorMessage(String errorMessage, String email, String password) {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_ERROR, errorMessage);
        intent.putExtra(LoginActivity.EXTRA_EMAIL, email);
        intent.putExtra(LoginActivity.EXTRA_PASSWORD, password);
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

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            String error = getString(R.string.login_empty);
            startLoginActivityWithErrorMessage(error, email, password);
        }
        inLoginProcess.set(true);
        new Thread(() -> {
            try {
                client.login(email, password);
                getdata();
            } catch (NetatmoOAuthException e) {
                String error = unwrapException(e);
                startLoginActivityWithErrorMessage(error, email, password);
            } finally {
                inLoginProcess.set(false);
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
            runOnUiThread(() -> binding.loadingIndicator.setVisibility(visibility));
        }
    }

    private boolean onUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private void showInfo(final ModuleVO moduleVO) {
        runOnUiThread(() -> {
            if (moduleVO.moduleType == ModuleVO.ModuleType.INDOOR) {
                binding.module1Name.setText(moduleVO.moduleName);
                binding.module1Timestamp.setText(getString(R.string.display_timestamp, moduleVO.getBeginTimeAsString()));
                binding.module1Temperature.setText(getString(R.string.display_temperature, moduleVO.temperature));
                binding.module1Humidity.setText(getString(R.string.display_humidity, moduleVO.humidity));
                binding.module1Co2.setText(getString(R.string.display_co2, moduleVO.co2));
            } else if (moduleVO.moduleType == ModuleVO.ModuleType.OUTDOOR) {
                binding.module2Name.setText(moduleVO.moduleName);
                binding.module2Timestamp.setText(getString(R.string.display_timestamp, moduleVO.getBeginTimeAsString()));
                binding.module2Temperature.setText(getString(R.string.display_temperature, moduleVO.temperature));
                binding.module2Humidity.setText(getString(R.string.display_humidity, moduleVO.humidity));
            }
        });
    }
}
