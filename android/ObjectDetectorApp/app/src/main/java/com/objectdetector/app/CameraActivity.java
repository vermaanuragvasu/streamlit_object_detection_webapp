package com.objectdetector.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.objectdetector.app.api.ApiClient;
import com.objectdetector.app.models.Detection;
import com.objectdetector.app.models.ImageDetectionResponse;
import com.objectdetector.app.views.BoundingBoxOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Real-time camera detection activity using CameraX.
 * Streams camera frames to the server for inference and overlays bounding boxes.
 */
public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private BoundingBoxOverlay boundingBoxOverlay;
    private TextView tvFps;
    private TextView tvDetectionCount;
    private MaterialButton btnToggleDetection;

    private ExecutorService analysisExecutor;
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private AtomicBoolean detectionEnabled = new AtomicBoolean(false);

    private long lastFrameTime = 0;
    private int frameCount = 0;
    private float currentFps = 0;

    private static final int FRAME_SKIP_INTERVAL = 5; // Process every 5th frame
    private int frameCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initViews();
        setupToolbar();

        analysisExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        btnToggleDetection.setOnClickListener(v -> {
            detectionEnabled.set(!detectionEnabled.get());
            if (detectionEnabled.get()) {
                btnToggleDetection.setText("Stop Detection");
            } else {
                btnToggleDetection.setText("Start Detection");
                boundingBoxOverlay.clear();
                tvDetectionCount.setText("Objects: -");
            }
        });
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        boundingBoxOverlay = findViewById(R.id.boundingBoxOverlay);
        tvFps = findViewById(R.id.tvFps);
        tvDetectionCount = findViewById(R.id.tvDetectionCount);
        btnToggleDetection = findViewById(R.id.btnToggleDetection);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Live Detection");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera init failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        frameCounter++;

        // Update FPS counter
        long now = System.currentTimeMillis();
        frameCount++;
        if (now - lastFrameTime >= 1000) {
            currentFps = frameCount * 1000f / (now - lastFrameTime);
            frameCount = 0;
            lastFrameTime = now;
            runOnUiThread(() -> tvFps.setText(String.format("FPS: %.1f", currentFps)));
        }

        if (!detectionEnabled.get() || isProcessing.get()
                || frameCounter % FRAME_SKIP_INTERVAL != 0) {
            imageProxy.close();
            return;
        }

        isProcessing.set(true);

        try {
            // Convert ImageProxy to JPEG bytes
            byte[] jpegBytes = imageProxyToJpeg(imageProxy);
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();

            if (jpegBytes != null) {
                sendFrameForDetection(jpegBytes, width, height);
            } else {
                isProcessing.set(false);
            }
        } catch (Exception e) {
            isProcessing.set(false);
        } finally {
            imageProxy.close();
        }
    }

    private byte[] imageProxyToJpeg(ImageProxy imageProxy) {
        try {
            // Convert YUV to JPEG using Android's built-in conversion
            android.graphics.ImageFormat.class.getName(); // ensure loaded
            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21, android.graphics.ImageFormat.NV21,
                    imageProxy.getWidth(), imageProxy.getHeight(), null);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new android.graphics.Rect(0, 0,
                            imageProxy.getWidth(), imageProxy.getHeight()),
                    80, out);

            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void sendFrameForDetection(byte[] jpegBytes, int width, int height) {
        try {
            File tempFile = File.createTempFile("frame_", ".jpg", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(jpegBytes);
            }

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("image/jpeg"), tempFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", tempFile.getName(), requestBody);

            ApiClient.getInstance(this).getApiService().detectImage(filePart)
                    .enqueue(new Callback<ImageDetectionResponse>() {
                        @Override
                        public void onResponse(Call<ImageDetectionResponse> call,
                                              Response<ImageDetectionResponse> response) {
                            isProcessing.set(false);
                            if (response.isSuccessful() && response.body() != null) {
                                ImageDetectionResponse result = response.body();
                                runOnUiThread(() -> {
                                    boundingBoxOverlay.setDetections(
                                            result.getDetections(),
                                            result.getImageWidth(),
                                            result.getImageHeight());
                                    tvDetectionCount.setText(
                                            "Objects: " + result.getDetections().size());
                                });
                            }
                            tempFile.delete();
                        }

                        @Override
                        public void onFailure(Call<ImageDetectionResponse> call, Throwable t) {
                            isProcessing.set(false);
                            tempFile.delete();
                        }
                    });
        } catch (IOException e) {
            isProcessing.set(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (analysisExecutor != null) {
            analysisExecutor.shutdown();
        }
    }
}
