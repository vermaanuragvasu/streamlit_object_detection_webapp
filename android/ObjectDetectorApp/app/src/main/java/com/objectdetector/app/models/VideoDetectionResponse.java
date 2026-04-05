package com.objectdetector.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * API response for video detection.
 */
public class VideoDetectionResponse {
    @SerializedName("total_frames")
    private int totalFrames;

    @SerializedName("processed_frames")
    private int processedFrames;

    @SerializedName("fps")
    private double fps;

    @SerializedName("processing_time_seconds")
    private double processingTimeSeconds;

    @SerializedName("output_video_url")
    private String outputVideoUrl;

    @SerializedName("frame_detections")
    private List<Map<String, Object>> frameDetections;

    public int getTotalFrames() { return totalFrames; }
    public void setTotalFrames(int totalFrames) { this.totalFrames = totalFrames; }

    public int getProcessedFrames() { return processedFrames; }
    public void setProcessedFrames(int processedFrames) { this.processedFrames = processedFrames; }

    public double getFps() { return fps; }
    public void setFps(double fps) { this.fps = fps; }

    public double getProcessingTimeSeconds() { return processingTimeSeconds; }
    public void setProcessingTimeSeconds(double processingTimeSeconds) {
        this.processingTimeSeconds = processingTimeSeconds;
    }

    public String getOutputVideoUrl() { return outputVideoUrl; }
    public void setOutputVideoUrl(String outputVideoUrl) { this.outputVideoUrl = outputVideoUrl; }

    public List<Map<String, Object>> getFrameDetections() { return frameDetections; }
    public void setFrameDetections(List<Map<String, Object>> frameDetections) {
        this.frameDetections = frameDetections;
    }
}
