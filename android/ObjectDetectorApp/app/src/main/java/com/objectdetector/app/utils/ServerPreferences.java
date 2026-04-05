package com.objectdetector.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.objectdetector.app.BuildConfig;

/**
 * Helper class to manage the server URL preference using SharedPreferences.
 * Allows users to configure the backend server address from the app.
 */
public class ServerPreferences {
    private static final String PREF_NAME = "object_detector_prefs";
    private static final String KEY_SERVER_URL = "server_url";

    private final SharedPreferences prefs;

    public ServerPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the saved server URL, or the default from BuildConfig if none is saved.
     */
    public String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, BuildConfig.API_BASE_URL);
    }

    /**
     * Save a new server URL.
     */
    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }

    /**
     * Check if a custom server URL has been configured.
     */
    public boolean hasCustomUrl() {
        return prefs.contains(KEY_SERVER_URL);
    }
}
