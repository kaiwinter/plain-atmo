package com.github.kaiwinter.myatmo.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.chart.ChartActivity;
import com.github.kaiwinter.myatmo.databinding.ActivityMainBinding;
import com.github.kaiwinter.myatmo.login.AccessTokenManager;
import com.github.kaiwinter.myatmo.login.LoginActivity;
import com.github.kaiwinter.myatmo.main.rest.StationsDataService;
import com.github.kaiwinter.myatmo.main.rest.model.Body;
import com.github.kaiwinter.myatmo.main.rest.model.Device;
import com.github.kaiwinter.myatmo.main.rest.model.Module;
import com.github.kaiwinter.myatmo.main.rest.model.StationsData;
import com.github.kaiwinter.myatmo.rest.APIError;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private String deviceId;
    private String indoorName;
    private String outdoorName;
    private String outdoorId;

    private SharedPreferencesStore preferencesStore;
    private AccessTokenManager accessTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferencesStore = new SharedPreferencesStore(this);
        accessTokenManager = new AccessTokenManager(this);
//        preferencesStore.setTokens(null, null, -1);
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
        if (TextUtils.isEmpty(preferencesStore.getAccessToken())) {
            startLoginActivity();
            return;
        }

//        if (!NetworkUtil.isOnline(this)) {
//            Snackbar.make(binding.getRoot(), R.string.no_connection, Snackbar.LENGTH_LONG).show();
//            return;
//        }

        showLoadingState();

        if (accessTokenManager.accessTokenRefreshNeeded()) {
            accessTokenManager.refreshAccessToken(this, this::getdata, errormessage -> {
                Snackbar.make(binding.getRoot(), errormessage, Snackbar.LENGTH_LONG).show();
                hideLoadingState();
            });
            return;
        }

        StationsDataService stationDataService = ServiceGenerator.createService(StationsDataService.class, preferencesStore.getAccessToken());
        Call<StationsData> stationsData = stationDataService.getStationsData(null);
        stationsData.enqueue(new Callback<StationsData>() {
            @Override
            public void onResponse(Call<StationsData> call, Response<StationsData> response) {
                if (response.isSuccessful()) {
                    StationsData stationsData = response.body();
                    handleSuccess(stationsData);
                } else {
                    APIError apiError = ServiceGenerator.parseError(response);
                    String detailMessage = apiError.error.message + " (" + apiError.error.code + ")";
                    Snackbar snackbar = Snackbar.make(binding.getRoot(), detailMessage, Snackbar.LENGTH_LONG);

                    if (response.code() == 401 || response.code() == 403) {
                        snackbar.setAction(R.string.logout_login, v -> {
                            preferencesStore.setTokens(null, null, -1);
                            startLoginActivity();
                        });
                    }
                    snackbar.show();
                }
                hideLoadingState();
            }

            private void handleSuccess(StationsData stationsData) {
                if (stationsData == null || stationsData.body == null) {
                    Snackbar.make(binding.getRoot(), R.string.no_station_data, Snackbar.LENGTH_LONG).show();
                    return;
                }

                Body body = stationsData.body;
                List<Device> devices = body.devices;

                if (devices.isEmpty()) {
                    Snackbar.make(binding.getRoot(), R.string.no_devices, Snackbar.LENGTH_LONG).show();
                    return;
                }
                Device device = devices.get(0);

                if (device.dashboardData == null) {
                    Snackbar.make(binding.getRoot(), R.string.no_dashboard_data, Snackbar.LENGTH_LONG).show();
                    return;
                }

                ModuleVO moduleVO = new ModuleVO();
                moduleVO.moduleName = device.moduleName;
                moduleVO.beginTime = device.dashboardData.timeUtc;
                moduleVO.temperature = device.dashboardData.temperature;
                moduleVO.humidity = device.dashboardData.humidity;

                deviceId = device.id;
                indoorName = device.moduleName;
                moduleVO.co2 = device.dashboardData.cO2;
                moduleVO.moduleType = ModuleVO.ModuleType.INDOOR;

                updateUiWithModuleData(moduleVO);

                // OUTDOOR
                List<Module> modules = device.modules;
                Iterator<Module> iterator = modules.iterator();
                while (iterator.hasNext()) {
                    Module module = iterator.next();
                    // Remove irrelevant modules
                    if (!"NAModule1".equals(module.type)) {
                        iterator.remove();
                    }
                }

                if (modules.size() == 1) {
                    showIndoorModuleData(modules.get(0));

                } else if (modules.size() > 1) {
                    String defaultIndoorModule = preferencesStore.getDefaultOutdoorModule();
                    if (defaultIndoorModule != null) {
                        boolean found = false;
                        for (Module module : modules) {
                            if (module.id.equals(defaultIndoorModule)) {
                                showIndoorModuleData(module);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            askUserAboutDefaultIndoorModule(modules);
                        }

                    } else {
                        askUserAboutDefaultIndoorModule(modules);
                    }
                }
            }

            private void askUserAboutDefaultIndoorModule(List<Module> modules) {
                List<String> moduleNames = new ArrayList<>();
                for (Module module : modules) {
                    moduleNames.add(module.moduleName);
                }

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.indoor_module_selection)
                        .setItems(moduleNames.toArray(new String[0]), (dialog1, position) -> {
                            Module module = modules.get(position);
                            preferencesStore.setDefaultOutdoorModule(module.id);

                            showIndoorModuleData(module);
                        });
                dialog.create().show();
            }

            private void showIndoorModuleData(Module module) {
                if (module.dashboardData == null) {
                    Snackbar.make(binding.getRoot(), R.string.no_dashboard_data, Snackbar.LENGTH_LONG).show();
                    return;
                }

                ModuleVO moduleVO = new ModuleVO();
                moduleVO.moduleName = module.moduleName;
                moduleVO.beginTime = module.dashboardData.timeUtc;
                moduleVO.temperature = module.dashboardData.temperature;
                moduleVO.humidity = module.dashboardData.humidity;

                outdoorId = module.id;
                outdoorName = module.moduleName;
                moduleVO.moduleType = ModuleVO.ModuleType.OUTDOOR;

                updateUiWithModuleData(moduleVO);
            }

            @Override
            public void onFailure(Call<StationsData> call, Throwable t) {
                Snackbar.make(binding.loadingIndicator, R.string.netatmo_connection_error, Snackbar.LENGTH_LONG).show();
                hideLoadingState();
            }
        });
    }

    private void showLoadingState() {
        runOnUiThread(() -> {
            binding.loadingIndicator.setVisibility(View.VISIBLE);
            binding.refreshButton.setEnabled(false);
        });
    }

    private void hideLoadingState() {
        runOnUiThread(() -> {
            binding.loadingIndicator.setVisibility(View.INVISIBLE);
            binding.refreshButton.setEnabled(true);
        });
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        // Make the app exit if back is pressed on main activity. Else the user returns to the Login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void updateUiWithModuleData(final ModuleVO moduleVO) {
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
