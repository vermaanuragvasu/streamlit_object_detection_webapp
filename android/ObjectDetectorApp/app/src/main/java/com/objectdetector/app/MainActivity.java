package com.objectdetector.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.objectdetector.app.api.ApiClient;
import com.objectdetector.app.utils.ServerPreferences;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main landing screen with options for Image Detection and Video Detection.
 * Includes server URL configuration via a settings button.
 */
public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardImageDetection;
    private MaterialCardView cardVideoDetection;
    private MaterialCardView cardLiveCamera;
    private View statusIndicator;
    private TextView tvServerStatus;
    private TextView tvServerUrl;
    private ServerPreferences serverPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverPreferences = new ServerPreferences(this);
        initViews();
        setupClickListeners();
        updateServerUrlDisplay();
        checkServerHealth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServerUrlDisplay();
        checkServerHealth();
    }

    private void initViews() {
        cardImageDetection = findViewById(R.id.cardImageDetection);
        cardVideoDetection = findViewById(R.id.cardVideoDetection);
        cardLiveCamera = findViewById(R.id.cardLiveCamera);
        statusIndicator = findViewById(R.id.statusIndicator);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvServerUrl = findViewById(R.id.tvServerUrl);
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

        // Settings button to configure server URL
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showServerUrlDialog());
        }
    }

    private void updateServerUrlDisplay() {
        if (tvServerUrl != null) {
            tvServerUrl.setText(serverPreferences.getServerUrl());
        }
    }

    private void showServerUrlDialog() {
        EditText input = new EditText(this);
        input.setText(serverPreferences.getServerUrl());
        input.setHint("http://192.168.1.100:8000");
        input.setSelectAllOnFocus(true);
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle("Server URL")
                .setMessage("Enter your backend server address.\nExample: http://192.168.1.100:8000")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        serverPreferences.setServerUrl(url);
                        ApiClient.resetInstance();
                        updateServerUrlDisplay();
                        checkServerHealth();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkServerHealth() {
        tvServerStatus.setText("Checking server...");
        statusIndicator.setBackgroundResource(R.drawable.status_indicator_yellow);

        ApiClient.getInstance(this).getApiService().healthCheck().enqueue(new Callback<ResponseBody>() {
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
