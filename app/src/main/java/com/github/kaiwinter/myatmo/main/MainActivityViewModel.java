package com.github.kaiwinter.myatmo.main;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.chart.rest.MeasureService;
import com.github.kaiwinter.myatmo.chart.rest.model.Measure;
import com.github.kaiwinter.myatmo.login.AccessTokenManager;
import com.github.kaiwinter.myatmo.login.ServiceError;
import com.github.kaiwinter.myatmo.main.rest.StationsDataService;
import com.github.kaiwinter.myatmo.main.rest.model.Body;
import com.github.kaiwinter.myatmo.main.rest.model.Device;
import com.github.kaiwinter.myatmo.main.rest.model.Module;
import com.github.kaiwinter.myatmo.main.rest.model.StationsData;
import com.github.kaiwinter.myatmo.rest.RestError;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;
import com.github.kaiwinter.myatmo.util.SingleLiveEvent;
import com.github.kaiwinter.myatmo.util.UserMessage;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivityViewModel extends AndroidViewModel {
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public MutableLiveData<ModuleVO> indoorModule = new MutableLiveData<>();
    public MutableLiveData<ModuleVO> outdoorModule = new MutableLiveData<>();

    public MutableLiveData<List<Entry>> indoorModuleTemperatureChartValues = new MutableLiveData<>();
    public MutableLiveData<List<Entry>> indoorModuleHumidityChartValues = new MutableLiveData<>();
    public MutableLiveData<List<Entry>> indoorModuleCo2ChartValues = new MutableLiveData<>();
    public MutableLiveData<List<Entry>> outdoorModuleTemperatureChartValues = new MutableLiveData<>();
    public MutableLiveData<List<Entry>> outdoorModuleHumidityChartValues = new MutableLiveData<>();

    SingleLiveEvent<Void> navigateToLoginActivity = new SingleLiveEvent<>();
    SingleLiveEvent<String> navigateToRelogin = new SingleLiveEvent<>();
    SingleLiveEvent<SwitchToChartActivityVO> navigateToChartActivity = new SingleLiveEvent<>();

    MutableLiveData<UserMessage> userMessage = new MutableLiveData<>();

    private final SharedPreferencesStore preferencesStore;
    private final AccessTokenManager accessTokenManager;
    private final StationsDataService stationDataService;

    public MainActivityViewModel(@NonNull Application application, SharedPreferencesStore preferencesStore, AccessTokenManager accessTokenManager, StationsDataService stationDataService) {
        super(application);
        this.preferencesStore = preferencesStore;
        this.accessTokenManager = accessTokenManager;
        this.stationDataService = stationDataService;
    }

    private void showLoadingState() {
        isLoading.postValue(true);
    }

    private void hideLoadingState() {
        isLoading.postValue(false);
    }

    public void getdata() {
        if (TextUtils.isEmpty(preferencesStore.getAccessToken())) {
            navigateToLoginActivity.call();
            return;
        }

        showLoadingState();

        if (accessTokenManager.accessTokenRefreshNeeded()) {
            accessTokenManager.refreshAccessToken(this::getdata, serviceError -> {
                if (serviceError.getErrorType() == ServiceError.ErrorType.NEED_TO_RELOGIN) {
                    navigateToRelogin.postValue(serviceError.getMessage());
                } else {
                    userMessage.postValue(UserMessage.create(serviceError.getMessage()));
                }
                hideLoadingState();
            });
            return;
        }

        Call<StationsData> stationsData = stationDataService.getStationsData("Bearer " + preferencesStore.getAccessToken(), null);
        stationsData.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<StationsData> call, Response<StationsData> response) {
                if (response.isSuccessful()) {
                    StationsData stationsData = response.body();
                    handleSuccess(stationsData);

                } else {
                    RestError.ApiError apiError = ServiceGenerator.parseError(response);
                    String detailMessage = response.code() + ": " + apiError.error.message + " (" + apiError.error.code + ")";

                    if (response.code() == 401 || response.code() == 403) {
                        navigateToRelogin.postValue(detailMessage);
                    } else {
                        userMessage.postValue(UserMessage.create(detailMessage));
                    }
                }
                hideLoadingState();
            }

            private void handleSuccess(StationsData stationsData) {
                if (stationsData == null || stationsData.body == null) {
                    userMessage.postValue(UserMessage.create(R.string.no_station_data));
                    return;
                }
                Body body = stationsData.body;
                List<Device> devices = body.devices;
                if (devices.isEmpty()) {
                    userMessage.postValue(UserMessage.create(R.string.no_devices));
                    return;
                }
                Device device = devices.get(0);
                showIndoorModuleData(device);

                List<Module> modules = device.modules;
                Iterator<Module> iterator = modules.iterator();
                while (iterator.hasNext()) {
                    Module module = iterator.next();
                    // Remove irrelevant modules
                    if (!"NAModule1".equals(module.type)) {
                        iterator.remove();
                    }
                }

                String outdoorModuleId = null;
                if (modules.size() == 1) {
                    showOutdoorModuleData(modules.get(0));
                    outdoorModuleId = modules.get(0).id;

                } else if (modules.size() > 1) {
                    // FIXME: move to activity, remove Application from constructor
                    String defaultIndoorModule = preferencesStore.getDefaultOutdoorModule();
                    if (defaultIndoorModule != null) {
                        boolean found = false;
                        for (Module module : modules) {
                            if (module.id.equals(defaultIndoorModule)) {
                                showOutdoorModuleData(module);
                                outdoorModuleId = module.id;
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

                loadMiniCharts(device.id, outdoorModuleId);
            }

            private void askUserAboutDefaultIndoorModule(List<Module> modules) {
                List<String> moduleNames = new ArrayList<>();
                for (Module module : modules) {
                    moduleNames.add(module.moduleName);
                }

                AlertDialog.Builder dialog = new AlertDialog.Builder(getApplication())
                        .setTitle(R.string.indoor_module_selection)
                        .setItems(moduleNames.toArray(new String[0]), (dialog1, position) -> {
                            Module module = modules.get(position);
                            preferencesStore.setDefaultOutdoorModule(module.id);

                            showOutdoorModuleData(module);
                        });
                dialog.create().show();
            }

            private void showIndoorModuleData(Device device) {
                ModuleVO moduleVO = new ModuleVO();
                moduleVO.moduleName = device.moduleName;
                moduleVO.id = device.id;
                moduleVO.moduleType = ModuleVO.ModuleType.INDOOR;

                if (!device.reachable) {
                    userMessage.postValue(UserMessage.create(R.string.module_not_reachable, device.moduleName));
                } else if (device.dashboardData == null) {
                    userMessage.postValue(UserMessage.create(R.string.no_dashboard_data));
                    return;
                } else {
                    moduleVO.beginTime = device.dashboardData.timeUtc;
                    moduleVO.temperature = device.dashboardData.temperature;
                    moduleVO.humidity = device.dashboardData.humidity;
                    moduleVO.co2 = device.dashboardData.cO2;
                }

                indoorModule.postValue(moduleVO);
            }

            private void showOutdoorModuleData(Module module) {
                ModuleVO moduleVO = new ModuleVO();
                moduleVO.moduleName = module.moduleName;
                moduleVO.id = module.id;
                moduleVO.moduleType = ModuleVO.ModuleType.OUTDOOR;

                if (!module.reachable) {
                    userMessage.postValue(UserMessage.create(R.string.module_not_reachable, module.moduleName));
                } else if (module.dashboardData == null) {
                    userMessage.postValue(UserMessage.create(R.string.no_dashboard_data));
                } else {
                    moduleVO.beginTime = module.dashboardData.timeUtc;
                    moduleVO.temperature = module.dashboardData.temperature;
                    moduleVO.humidity = module.dashboardData.humidity;
                }
                outdoorModule.postValue(moduleVO);
            }

            @Override
            public void onFailure(Call<StationsData> call, Throwable t) {
                userMessage.postValue(UserMessage.create(R.string.netatmo_connection_error, t.getMessage()));
                hideLoadingState();
            }
        });
    }

    public void loadMiniCharts(String deviceId, String outdoorModuleId) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date startDate = calendar.getTime();

        SharedPreferencesStore preferencesStore = new SharedPreferencesStore(getApplication());
        MeasureService service = ServiceGenerator.createService(MeasureService.class);

        Call<Measure> indoorModuleCall = service.getMeasure(
                "Bearer " + preferencesStore.getAccessToken(),
                deviceId,
                null,
                MeasurementType.SCALE_THIRTY_MINUTES.getApiString(),
                MeasurementType.TYPE_TEMPERATURE.getApiString() + "," + MeasurementType.TYPE_HUMIDITY.getApiString() + "," + MeasurementType.TYPE_CO2.getApiString(),
                (int) (startDate.getTime() / 1000),
                false);
        indoorModuleCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Measure> call, Response<Measure> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                Measure responseBody = response.body();

                List<Entry> temperatureValues = responseBody.body.toEntry(0);
                indoorModuleTemperatureChartValues.postValue(temperatureValues);

                List<Entry> humidityValues = responseBody.body.toEntry(1);
                indoorModuleHumidityChartValues.postValue(humidityValues);

                List<Entry> co2Values = responseBody.body.toEntry(2);
                indoorModuleCo2ChartValues.postValue(co2Values);
            }

            @Override
            public void onFailure(Call<Measure> call, Throwable t) {
                Log.e(MainActivityViewModel.class.getSimpleName(), "onFailure", t);
            }
        });

        Call<Measure> outdoorModuleCall = service.getMeasure(
                "Bearer " + preferencesStore.getAccessToken(),
                deviceId,
                outdoorModuleId,
                MeasurementType.SCALE_THIRTY_MINUTES.getApiString(),
                MeasurementType.TYPE_TEMPERATURE.getApiString() + "," + MeasurementType.TYPE_HUMIDITY.getApiString(),
                (int) (startDate.getTime() / 1000),
                false);
        outdoorModuleCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Measure> call, Response<Measure> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                Measure responseBody = response.body();

                List<Entry> temperatureValues = responseBody.body.toEntry(0);
                outdoorModuleTemperatureChartValues.postValue(temperatureValues);

                List<Entry> humidityValues = responseBody.body.toEntry(1);
                outdoorModuleHumidityChartValues.postValue(humidityValues);
            }

            @Override
            public void onFailure(Call<Measure> call, Throwable t) {
                Log.e(MainActivityViewModel.class.getSimpleName(), "onFailure", t);
            }
        });
    }

    public void clearTokens() {
        preferencesStore.setTokens(null, null, -1);
    }

    public void navigateToChartActivity(StationType stationType, MeasurementType measurementType) {
        SwitchToChartActivityVO switchToChartActivityVO = new SwitchToChartActivityVO(stationType, measurementType);
        navigateToChartActivity.postValue(switchToChartActivityVO);
    }
}