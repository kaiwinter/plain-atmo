package com.github.kaiwinter.myatmo.main;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.kaiwinter.myatmo.login.AccessTokenManager;
import com.github.kaiwinter.myatmo.main.rest.StationsDataService;
import com.github.kaiwinter.myatmo.rest.ServiceGenerator;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;

public class MainActivityViewModelFactory implements ViewModelProvider.Factory {
    private Application mApplication;

    public MainActivityViewModelFactory(Application application) {
        mApplication = application;
    }


    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.equals(MainActivityViewModel.class)) {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStore(mApplication);
            StationsDataService stationDataService = ServiceGenerator.createService(StationsDataService.class, sharedPreferencesStore.getAccessToken());
            return (T) new MainActivityViewModel(mApplication, sharedPreferencesStore, new AccessTokenManager(mApplication), stationDataService);
        }
        return null;
    }
}