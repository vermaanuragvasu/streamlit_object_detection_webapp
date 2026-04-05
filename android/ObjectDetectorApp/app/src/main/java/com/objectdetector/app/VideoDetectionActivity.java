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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.objectdetector.app.api.ApiClient;
import com.objectdetector.app.models.VideoDetectionResponse;
import com.objectdetector.app.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for video-based object detection.
 * Supports uploading from gallery or recording from camera.
 * Shows processed video with bounding boxes and real-time FPS.
 */
public class VideoDetectionActivity extends AppCompatActivity {

    private PlayerView playerView;
    private PlayerView resultPlayerView;
    private ExoPlayer sourcePlayer;
    private ExoPlayer resultPlayer;
    private MaterialButton btnUploadVideo;
    private MaterialButton btnRecordVideo;
    private MaterialButton btnDetectVideo;
    private MaterialButton btnExportVideo;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvVideoResults;
    private View resultsContainer;

    private Uri selectedVideoUri;
    private Uri recordedVideoUri;
    private String processedVideoUrl;

    private final ActivityResultLauncher<String> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedVideoUri = uri;
                    playSourceVideo(uri);
                    btnDetectVideo.setEnabled(true);
                    resultsContainer.setVisibility(View.GONE);
                }
            });

    private final ActivityResultLauncher<Uri> videoRecordLauncher =
            registerForActivityResult(new ActivityResultContracts.CaptureVideo(), success -> {
                if (success && recordedVideoUri != null) {
                    selectedVideoUri = recordedVideoUri;
                    playSourceVideo(recordedVideoUri);
                    btnDetectVideo.setEnabled(true);
                    resultsContainer.setVisibility(View.GONE);
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                if (Boolean.TRUE.equals(cameraGranted)) {
                    launchVideoRecorder();
                } else {
                    Toast.makeText(this, "Camera permission required for recording",
                            Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detection);

        initViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
        resultPlayerView = findViewById(R.id.resultPlayerView);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        btnDetectVideo = findViewById(R.id.btnDetectVideo);
        btnExportVideo = findViewById(R.id.btnExportVideo);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvVideoResults = findViewById(R.id.tvVideoResults);
        resultsContainer = findViewById(R.id.resultsContainer);

        btnDetectVideo.setEnabled(false);
        btnExportVideo.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Video Detection");
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
        btnUploadVideo.setOnClickListener(v -> videoPickerLauncher.launch("video/*"));

        btnRecordVideo.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchVideoRecorder();
            } else {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                });
            }
        });

        btnDetectVideo.setOnClickListener(v -> processVideo());

        btnExportVideo.setOnClickListener(v -> exportProcessedVideo());
    }

    private void launchVideoRecorder() {
        try {
            File videoFile = createVideoFile();
            recordedVideoUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    videoFile
            );
            videoRecordLauncher.launch(recordedVideoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating video file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String videoFileName = "VID_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        return File.createTempFile(videoFileName, ".mp4", storageDir);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playSourceVideo(Uri uri) {
        if (sourcePlayer != null) {
            sourcePlayer.release();
        }
        sourcePlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(sourcePlayer);
        playerView.setVisibility(View.VISIBLE);

        MediaItem mediaItem = MediaItem.fromUri(uri);
        sourcePlayer.setMediaItem(mediaItem);
        sourcePlayer.prepare();
        sourcePlayer.setPlayWhenReady(false);

        tvStatus.setText("Video loaded. Tap Process to detect objects.");
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playResultVideo(String url) {
        if (resultPlayer != null) {
            resultPlayer.release();
        }
        resultPlayer = new ExoPlayer.Builder(this).build();
        resultPlayerView.setPlayer(resultPlayer);
        resultPlayerView.setVisibility(View.VISIBLE);

        String fullUrl = ApiClient.getInstance().getBaseUrl() + url;
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(fullUrl));
        resultPlayer.setMediaItem(mediaItem);
        resultPlayer.prepare();
        resultPlayer.setPlayWhenReady(true);
    }

    private void processVideo() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please select a video first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvStatus.setText("Processing video... This may take a while.");

        try {
            File videoFile = FileUtils.copyUriToTempFile(this, selectedVideoUri, "vid_");
            String mimeType = FileUtils.getMimeType(this, selectedVideoUri);
            if (mimeType == null) mimeType = "video/mp4";

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse(mimeType), videoFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", videoFile.getName(), requestBody);

            ApiClient.getInstance().getApiService().detectVideo(filePart)
                    .enqueue(new Callback<VideoDetectionResponse>() {
                        @Override
                        public void onResponse(Call<VideoDetectionResponse> call,
                                              Response<VideoDetectionResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                displayVideoResults(response.body());
                            } else {
                                tvStatus.setText("Video processing failed.");
                                Snackbar.make(playerView, "Processing failed",
                                        Snackbar.LENGTH_LONG).show();
                            }
                            videoFile.delete();
                        }

                        @Override
                        public void onFailure(Call<VideoDetectionResponse> call, Throwable t) {
                            setLoading(false);
                            tvStatus.setText("Connection error.");
                            Snackbar.make(playerView,
                                    "Network error: " + t.getMessage(),
                                    Snackbar.LENGTH_LONG).show();
                            videoFile.delete();
                        }
                    });
        } catch (IOException e) {
            setLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayVideoResults(VideoDetectionResponse response) {
        processedVideoUrl = response.getOutputVideoUrl();

        // Play the processed video
        playResultVideo(processedVideoUrl);

        // Show results
        resultsContainer.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total frames: %d\n", response.getTotalFrames()));
        sb.append(String.format("Processed frames: %d\n", response.getProcessedFrames()));
        sb.append(String.format("Processing FPS: %.1f\n", response.getFps()));
        sb.append(String.format("Processing time: %.1f seconds\n", response.getProcessingTimeSeconds()));

        tvVideoResults.setText(sb.toString());
        tvStatus.setText("Video processed successfully!");

        btnExportVideo.setVisibility(View.VISIBLE);
    }

    private void exportProcessedVideo() {
        if (processedVideoUrl == null) {
            Toast.makeText(this, "No processed video to export", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvStatus.setText("Downloading processed video...");

        String fullUrl = ApiClient.getInstance().getBaseUrl() + processedVideoUrl;
        ApiClient.getInstance().getApiService().downloadVideo(fullUrl)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                File outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                                String filename = "detected_" + System.currentTimeMillis() + ".mp4";
                                File outputFile = new File(outputDir, filename);

                                try (InputStream is = response.body().byteStream();
                                     FileOutputStream fos = new FileOutputStream(outputFile)) {
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    while ((bytesRead = is.read(buffer)) != -1) {
                                        fos.write(buffer, 0, bytesRead);
                                    }
                                }

                                tvStatus.setText("Video saved: " + outputFile.getAbsolutePath());
                                Toast.makeText(VideoDetectionActivity.this,
                                        "Video exported successfully!", Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                tvStatus.setText("Failed to save video.");
                            }
                        } else {
                            tvStatus.setText("Download failed.");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        setLoading(false);
                        tvStatus.setText("Download error.");
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnDetectVideo.setEnabled(!loading && selectedVideoUri != null);
        btnUploadVideo.setEnabled(!loading);
        btnRecordVideo.setEnabled(!loading);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sourcePlayer != null) sourcePlayer.pause();
        if (resultPlayer != null) resultPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sourcePlayer != null) {
            sourcePlayer.release();
            sourcePlayer = null;
        }
        if (resultPlayer != null) {
            resultPlayer.release();
            resultPlayer = null;
        }
    }
}
