package com.github.kaiwinter.myatmo.main;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import losty.netatmo.store.TokenStore;

public class SharedPreferencesTokenStore implements TokenStore {

    private static final String KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN";
    private static final String KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN";
    private static final String KEY_EXPIRES_AT = "KEY_EXPIRES_AT";
    private final SharedPreferences sharedPreferences;

    SharedPreferencesTokenStore(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void setTokens(String refreshToken, String accessToken, long expiresAt) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putLong(KEY_EXPIRES_AT, expiresAt);
        editor.apply();
    }

    @Override
    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    @Override
    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    @Override
    public long getExpiresAt() {
        return sharedPreferences.getLong(KEY_EXPIRES_AT, 0);
    }
}
