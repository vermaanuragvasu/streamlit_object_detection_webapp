package com.objectdetector.app.api;

import com.objectdetector.app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton API client using Retrofit for network communication.
 */
public class ApiClient {
    private static ApiClient instance;
    private final ApiService apiService;
    private String baseUrl;

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

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient(BuildConfig.API_BASE_URL);
        }
        return instance;
    }

    public static synchronized ApiClient getInstance(String baseUrl) {
        if (instance == null || !instance.baseUrl.equals(baseUrl)) {
            instance = new ApiClient(baseUrl);
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
