package com.objectdetector.app.api;

import android.content.Context;

import com.objectdetector.app.BuildConfig;
import com.objectdetector.app.utils.ServerPreferences;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton API client using Retrofit for network communication.
 * Uses SharedPreferences to store and retrieve the server URL,
 * allowing users to configure the backend address from the app.
 */
public class ApiClient {
    private static ApiClient instance;
    private final ApiService apiService;
    private final String baseUrl;

    private ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    /**
     * Get the ApiClient instance using the server URL from SharedPreferences.
     * This is the preferred method — it reads the user-configured URL.
     */
    public static synchronized ApiClient getInstance(Context context) {
        String url = new ServerPreferences(context).getServerUrl();
        if (instance == null || !instance.baseUrl.equals(url)) {
            instance = new ApiClient(url);
        }
        return instance;
    }

    /**
     * Get the ApiClient instance using the default BuildConfig URL.
     * Fallback for cases where Context is not available.
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient(BuildConfig.API_BASE_URL);
        }
        return instance;
    }

    /**
     * Force re-creation of the ApiClient with a new URL.
     * Called when the user changes the server URL in settings.
     */
    public static synchronized void resetInstance() {
        instance = null;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
