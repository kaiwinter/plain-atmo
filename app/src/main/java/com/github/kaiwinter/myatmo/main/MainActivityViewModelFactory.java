package com.github.kaiwinter.myatmo.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.kaiwinter.myatmo.login.AccessTokenManager;
import com.github.kaiwinter.myatmo.main.rest.StationsDataService;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;

public class MainActivityViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public MainActivityViewModelFactory(Application application) {
        this.application = application;
    }


    @NonNull
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.equals(MainActivityViewModel.class)) {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStore(application);
            StationsDataService stationDataService = ServiceGenerator.createService(StationsDataService.class);
            return (T) new MainActivityViewModel(application, sharedPreferencesStore, new AccessTokenManager(application), stationDataService);
        }
        throw new RuntimeException("Cannot create an instance of " + modelClass);
    }
}