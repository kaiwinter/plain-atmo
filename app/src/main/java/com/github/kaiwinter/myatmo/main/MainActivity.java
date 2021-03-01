package com.github.kaiwinter.myatmo.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.chart.ChartActivity;
import com.github.kaiwinter.myatmo.databinding.ActivityMainBinding;
import com.github.kaiwinter.myatmo.login.LoginActivity;
import com.github.kaiwinter.myatmo.login.rest.LoginService;
import com.github.kaiwinter.myatmo.login.rest.model.AccessToken;
import com.github.kaiwinter.myatmo.main.rest.StationsDataService;
import com.github.kaiwinter.myatmo.main.rest.model.Body;
import com.github.kaiwinter.myatmo.main.rest.model.Device;
import com.github.kaiwinter.myatmo.main.rest.model.Module;
import com.github.kaiwinter.myatmo.main.rest.model.StationsData;
import com.github.kaiwinter.myatmo.rest.APIError;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesTokenStore;
import com.github.kaiwinter.myatmo.util.NetworkUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final long EXPIRE_TOLERANCE_SECONDS = 60;

    private ActivityMainBinding binding;

    private String deviceId;
    private String indoorName;
    private String outdoorName;
    private String outdoorId;

    private SharedPreferencesTokenStore tokenstore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenstore = new SharedPreferencesTokenStore(this);
//        tokenstore.setTokens(null, null, -1);
    }

    /**
     * Called from the refresh button defined in the XML.
     */
    public void refreshButtonClicked(View view) {
        getdata();
    }

    public void detailButtonClicked(View view) {
        if (view == binding.module1TemperatureCard) {
            showIndoorChart(Params.TYPE_TEMPERATURE);
        } else if (view == binding.module1HumidityCard) {
            showIndoorChart(Params.TYPE_HUMIDITY);
        } else if (view == binding.module1Co2Card) {
            showIndoorChart(Params.TYPE_CO2);
        } else if (view == binding.module2TemperatureCard) {
            showOutdoorChart(Params.TYPE_TEMPERATURE);
        } else if (view == binding.module2HumidityCard) {
            showOutdoorChart(Params.TYPE_HUMIDITY);
        }
    }

    private void showIndoorChart(String measurementType) {
        Intent intent = new Intent(getApplicationContext(), ChartActivity.class);
        intent.putExtra(ChartActivity.DEVICE_ID, deviceId);
        intent.putExtra(ChartActivity.MODULE_NAME, indoorName);
        intent.putExtra(ChartActivity.MEASUREMENT_TYPE, measurementType);
        startActivity(intent);
    }

    private void showOutdoorChart(String measurementType) {
        Intent intent = new Intent(getApplicationContext(), ChartActivity.class);
        intent.putExtra(ChartActivity.DEVICE_ID, deviceId);
        intent.putExtra(ChartActivity.MODULE_ID, outdoorId);
        intent.putExtra(ChartActivity.MODULE_NAME, outdoorName);
        intent.putExtra(ChartActivity.MEASUREMENT_TYPE, measurementType);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getdata();
    }

    private void getdata() {
        if (TextUtils.isEmpty(tokenstore.getAccessToken())) {
            startLoginActivity();
            return;
        }
        if (!NetworkUtil.isOnline(this)) {
            Snackbar.make(binding.getRoot(), R.string.no_connection, Snackbar.LENGTH_LONG).show();
            return;
        }
        runOnUiThread(() -> {
            binding.loadingIndicator.setVisibility(View.VISIBLE);
            binding.refreshButton.setEnabled(false);
        });

        if (accessTokenRefreshNeeded()) {
            Snackbar.make(binding.getRoot(), "refreshing access token", Snackbar.LENGTH_LONG).show();
            refreshAccessToken(errormessage -> Snackbar.make(binding.getRoot(), errormessage, Snackbar.LENGTH_LONG).show());
            return;
        }

        StationsDataService stationDataService = ServiceGenerator.createService(StationsDataService.class, tokenstore.getAccessToken());
        Call<StationsData> stationsData = stationDataService.getStationsData(null);
        stationsData.enqueue(new Callback<StationsData>() {
            @Override
            public void onResponse(Call<StationsData> call, Response<StationsData> response) {
                if (response.code() == 200) {
                    StationsData stationsData = response.body();
                    Body body = stationsData.body;
                    if (body != null) {
                        List<Device> devices = body.devices;
                        Device device = devices.get(0);

                        ModuleVO moduleVO = new ModuleVO();
                        moduleVO.moduleName = device.moduleName;
                        moduleVO.beginTime = device.dashboardData.timeUtc;
                        moduleVO.temperature = device.dashboardData.temperature;
                        moduleVO.humidity = device.dashboardData.humidity;

                        deviceId = device.id;
                        indoorName = device.moduleName;
                        moduleVO.co2 = device.dashboardData.cO2;
                        moduleVO.moduleType = ModuleVO.ModuleType.INDOOR;

                        showInfo(moduleVO);

                        // OUTDOOR
                        List<Module> modules = device.modules;
                        Module module = modules.get(0);

                        ModuleVO moduleVO2 = new ModuleVO();
                        moduleVO2.moduleName = module.moduleName;
                        moduleVO2.beginTime = module.dashboardData.timeUtc;
                        moduleVO2.temperature = module.dashboardData.temperature;
                        moduleVO2.humidity = module.dashboardData.humidity;

                        outdoorId = module.id;
                        outdoorName = module.moduleName;
                        moduleVO2.moduleType = ModuleVO.ModuleType.OUTDOOR;

                        showInfo(moduleVO2);
                    }
                    runOnUiThread(() -> {
                        binding.loadingIndicator.setVisibility(View.INVISIBLE);
                        binding.refreshButton.setEnabled(true);
                    });

                } else {
                    APIError apiError = ServiceGenerator.parseError(response);
                    String detailMessage = apiError.error.message + "(" + apiError.error.code + ")";
                    Snackbar snackbar = Snackbar.make(binding.getRoot(), detailMessage, Snackbar.LENGTH_LONG);

                    if (response.code() == 401 || response.code() == 403) {
                        snackbar.setAction("Neu einloggen", v -> {
                            tokenstore.setTokens(null, null, -1);
                            startLoginActivity();
                        });
                    }
                    snackbar.show();
                    runOnUiThread(() -> {
                        binding.loadingIndicator.setVisibility(View.INVISIBLE);
                        binding.refreshButton.setEnabled(true);
                    });
                }
            }

            @Override
            public void onFailure(Call<StationsData> call, Throwable t) {
                runOnUiThread(() -> {
                    String message = getString(R.string.main_load_error, t.getMessage());
                    Snackbar.make(binding.loadingIndicator, message, Snackbar.LENGTH_LONG).show();

                    binding.loadingIndicator.setVisibility(View.INVISIBLE);
                    binding.refreshButton.setEnabled(true);
                });
            }
        });
    }

    private boolean accessTokenRefreshNeeded() {
        long expiresAt = tokenstore.getExpiresAt();
        long currentTimestamp = new Date().getTime() / 1000;

        return currentTimestamp + EXPIRE_TOLERANCE_SECONDS >= expiresAt;
    }

    private void refreshAccessToken(Consumer<String> onError) {
        LoginService loginService = ServiceGenerator.createService(LoginService.class);

        String clientId = getString(R.string.client_id);
        String clientSecret = getString(R.string.client_secret);
        String refreshToken = tokenstore.getRefreshToken();
        Call<AccessToken> call = loginService.refreshToken(clientId, clientSecret, "refresh_token", refreshToken);
        call.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                Log.e(TAG, "success");

                if (response.code() == 200) {
                    AccessToken body = response.body();
                    long expiresAt = System.currentTimeMillis() / 1000 + body.expiresIn;
                    tokenstore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                    getdata(); // retry loading
                } else {
                    APIError apiError = ServiceGenerator.parseError(response);
                    String errormessage = apiError.error.message + " (" + response.code() + ", " + apiError.error.code + ")";
                    onError.accept(errormessage);
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                String errormessage = getString(R.string.main_load_error, t.getMessage());
                onError.accept(errormessage);
            }
        });
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivityForResult(intent, 0);
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
