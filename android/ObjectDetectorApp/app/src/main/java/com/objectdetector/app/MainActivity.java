package com.objectdetector.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.objectdetector.app.api.ApiClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main landing screen with options for Image Detection and Video Detection.
 */
public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardImageDetection;
    private MaterialCardView cardVideoDetection;
    private MaterialCardView cardLiveCamera;
    private View statusIndicator;
    private android.widget.TextView tvServerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        checkServerHealth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServerHealth();
    }

    private void initViews() {
        cardImageDetection = findViewById(R.id.cardImageDetection);
        cardVideoDetection = findViewById(R.id.cardVideoDetection);
        cardLiveCamera = findViewById(R.id.cardLiveCamera);
        statusIndicator = findViewById(R.id.statusIndicator);
        tvServerStatus = findViewById(R.id.tvServerStatus);
    }

    private void setupClickListeners() {
        cardImageDetection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageDetectionActivity.class);
            startActivity(intent);
        });

        cardVideoDetection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VideoDetectionActivity.class);
            startActivity(intent);
        });

        cardLiveCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });
    }

    private void checkServerHealth() {
        tvServerStatus.setText("Checking server...");
        statusIndicator.setBackgroundResource(R.drawable.status_indicator_yellow);

        ApiClient.getInstance().getApiService().healthCheck().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    tvServerStatus.setText("Server online");
                    statusIndicator.setBackgroundResource(R.drawable.status_indicator_green);
                } else {
                    tvServerStatus.setText("Server error");
                    statusIndicator.setBackgroundResource(R.drawable.status_indicator_red);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                tvServerStatus.setText("Server offline");
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_red);
            }
        });
    }
}
