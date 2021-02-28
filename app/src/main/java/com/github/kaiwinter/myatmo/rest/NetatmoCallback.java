package com.github.kaiwinter.myatmo.rest;

import android.app.Activity;
import android.os.Looper;
import android.view.View;

import com.github.kaiwinter.myatmo.R;
import com.google.android.material.snackbar.Snackbar;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetatmoCallback<T> implements Callback<T> {

    private final Activity context;
    private final SmoothProgressBar loadingIndicator;

    public NetatmoCallback(Activity context, SmoothProgressBar loadingIndicator) {
        this.context = context;
        this.loadingIndicator = loadingIndicator;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {

    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        String message = context.getString(R.string.login_login_error, t.getMessage());
        Snackbar.make(loadingIndicator, message, Snackbar.LENGTH_LONG).show();
        changeLoadingIndicatorVisibility(View.INVISIBLE);
    }

    private void changeLoadingIndicatorVisibility(final int visibility) {
        if (onUiThread()) {
            loadingIndicator.setVisibility(visibility);
        } else {
            context.runOnUiThread(() -> loadingIndicator.setVisibility(visibility));
        }
    }

    private boolean onUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

}
