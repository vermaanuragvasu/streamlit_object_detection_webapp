package com.objectdetector.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response for image detection.
 */
public class ImageDetectionResponse {
    @SerializedName("detections")
    private List<Detection> detections;

    @SerializedName("image_width")
    private int imageWidth;

    @SerializedName("image_height")
    private int imageHeight;

    @SerializedName("processing_time_ms")
    private double processingTimeMs;

    public List<Detection> getDetections() { return detections; }
    public void setDetections(List<Detection> detections) { this.detections = detections; }

    public int getImageWidth() { return imageWidth; }
    public void setImageWidth(int imageWidth) { this.imageWidth = imageWidth; }

    public int getImageHeight() { return imageHeight; }
    public void setImageHeight(int imageHeight) { this.imageHeight = imageHeight; }

    public double getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(double processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
