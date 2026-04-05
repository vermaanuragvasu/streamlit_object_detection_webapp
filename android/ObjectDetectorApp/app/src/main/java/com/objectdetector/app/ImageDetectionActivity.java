package com.objectdetector.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.objectdetector.app.api.ApiClient;
import com.objectdetector.app.models.Detection;
import com.objectdetector.app.models.ImageDetectionResponse;
import com.objectdetector.app.utils.FileUtils;
import com.objectdetector.app.views.BoundingBoxOverlay;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for image-based object detection.
 * Supports uploading from gallery or capturing from camera.
 */
public class ImageDetectionActivity extends AppCompatActivity {

    private ImageView imageView;
    private BoundingBoxOverlay boundingBoxOverlay;
    private MaterialButton btnGallery;
    private MaterialButton btnCamera;
    private MaterialButton btnDetect;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvResults;
    private View resultsContainer;

    private Uri selectedImageUri;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    displaySelectedImage(uri);
                    btnDetect.setEnabled(true);
                    boundingBoxOverlay.clear();
                    resultsContainer.setVisibility(View.GONE);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    displaySelectedImage(cameraImageUri);
                    btnDetect.setEnabled(true);
                    boundingBoxOverlay.clear();
                    resultsContainer.setVisibility(View.GONE);
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detection);

        initViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initViews() {
        imageView = findViewById(R.id.imageView);
        boundingBoxOverlay = findViewById(R.id.boundingBoxOverlay);
        btnGallery = findViewById(R.id.btnGallery);
        btnCamera = findViewById(R.id.btnCamera);
        btnDetect = findViewById(R.id.btnDetect);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvResults = findViewById(R.id.tvResults);
        resultsContainer = findViewById(R.id.resultsContainer);

        btnDetect.setEnabled(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Image Detection");
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

    private void setupClickListeners() {
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnDetect.setOnClickListener(v -> detectObjects());
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void displaySelectedImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .into(imageView);
        tvStatus.setText("Image loaded. Tap Detect to analyze.");
    }

    private void detectObjects() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvStatus.setText("Detecting objects...");

        try {
            File imageFile = FileUtils.copyUriToTempFile(this, selectedImageUri, "img_");
            String mimeType = FileUtils.getMimeType(this, selectedImageUri);
            if (mimeType == null) mimeType = "image/jpeg";

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse(mimeType), imageFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", imageFile.getName(), requestBody);

            ApiClient.getInstance().getApiService().detectImage(filePart)
                    .enqueue(new Callback<ImageDetectionResponse>() {
                        @Override
                        public void onResponse(Call<ImageDetectionResponse> call,
                                              Response<ImageDetectionResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                displayResults(response.body());
                            } else {
                                tvStatus.setText("Detection failed. Server returned an error.");
                                Snackbar.make(imageView, "Detection failed", Snackbar.LENGTH_LONG).show();
                            }
                            imageFile.delete();
                        }

                        @Override
                        public void onFailure(Call<ImageDetectionResponse> call, Throwable t) {
                            setLoading(false);
                            tvStatus.setText("Connection error. Is the server running?");
                            Snackbar.make(imageView,
                                    "Network error: " + t.getMessage(),
                                    Snackbar.LENGTH_LONG).show();
                            imageFile.delete();
                        }
                    });
        } catch (IOException e) {
            setLoading(false);
            tvStatus.setText("Error reading image file.");
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayResults(ImageDetectionResponse response) {
        List<Detection> detections = response.getDetections();

        // Draw bounding boxes
        boundingBoxOverlay.setDetections(
                detections,
                response.getImageWidth(),
                response.getImageHeight()
        );

        // Show results text
        resultsContainer.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Processing time: %.1f ms\n", response.getProcessingTimeMs()));
        sb.append(String.format("Objects detected: %d\n\n", detections.size()));

        for (Detection det : detections) {
            sb.append(String.format("  %s — %s\n", det.getLabel(), det.getFormattedConfidence()));
        }

        tvResults.setText(sb.toString());
        tvStatus.setText(String.format("Detected %d object(s)", detections.size()));
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnDetect.setEnabled(!loading);
        btnGallery.setEnabled(!loading);
        btnCamera.setEnabled(!loading);
    }
}
