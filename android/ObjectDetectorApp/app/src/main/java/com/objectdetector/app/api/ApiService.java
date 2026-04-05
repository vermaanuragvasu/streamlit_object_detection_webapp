package com.objectdetector.app.api;

import com.objectdetector.app.models.ImageDetectionResponse;
import com.objectdetector.app.models.VideoDetectionResponse;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Retrofit API service interface for the Object Detection backend.
 */
public interface ApiService {

    @Multipart
    @POST("detect/image")
    Call<ImageDetectionResponse> detectImage(@Part MultipartBody.Part file);

    @Multipart
    @POST("detect/video")
    Call<VideoDetectionResponse> detectVideo(@Part MultipartBody.Part file);

    @Streaming
    @GET
    Call<ResponseBody> downloadVideo(@Url String url);

    @GET("health")
    Call<ResponseBody> healthCheck();
}
