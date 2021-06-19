package com.github.kaiwinter.myatmo.main;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.login.AccessTokenManager;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivityViewModel extends AndroidViewModel {
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    MutableLiveData<String> deviceId = new MutableLiveData<>();

    public MutableLiveData<ModuleVO> indoorModule = new MutableLiveData<>();
    public MutableLiveData<ModuleVO> outdoorModule = new MutableLiveData<>();

    SingleLiveEvent<Void> navigateToLoginActivity = new SingleLiveEvent<>();
    SingleLiveEvent<String> navigateToRelogin = new SingleLiveEvent<>();

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
            accessTokenManager.refreshAccessToken(this::getdata, errormessage -> {
                navigateToRelogin.postValue(errormessage);
            });
            return;
        }

        Call<StationsData> stationsData = stationDataService.getStationsData("Bearer " + preferencesStore.getAccessToken(), null);
        stationsData.enqueue(new Callback<StationsData>() {
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

                if (modules.size() == 1) {
                    showOutdoorModuleData(modules.get(0));

                } else if (modules.size() > 1) {
                    // FIXME: move to activity, remove Application from constructor
                    String defaultIndoorModule = preferencesStore.getDefaultOutdoorModule();
                    if (defaultIndoorModule != null) {
                        boolean found = false;
                        for (Module module : modules) {
                            if (module.id.equals(defaultIndoorModule)) {
                                showOutdoorModuleData(module);
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
                deviceId.postValue(device.id); // Refactor to moduleVo.id?
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

    public void clearTokens() {
        preferencesStore.setTokens(null, null, -1);
    }
}