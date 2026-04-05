package com.objectdetector.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents a single detected object from the API.
 */
public class Detection {
    @SerializedName("label")
    private String label;

    @SerializedName("confidence")
    private double confidence;

    @SerializedName("bbox")
    private List<Double> bbox; // [x1, y1, x2, y2]

    public Detection() {}

    public Detection(String label, double confidence, List<Double> bbox) {
        this.label = label;
        this.confidence = confidence;
        this.bbox = bbox;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public List<Double> getBbox() { return bbox; }
    public void setBbox(List<Double> bbox) { this.bbox = bbox; }

    public float getX1() { return bbox != null && bbox.size() >= 4 ? bbox.get(0).floatValue() : 0; }
    public float getY1() { return bbox != null && bbox.size() >= 4 ? bbox.get(1).floatValue() : 0; }
    public float getX2() { return bbox != null && bbox.size() >= 4 ? bbox.get(2).floatValue() : 0; }
    public float getY2() { return bbox != null && bbox.size() >= 4 ? bbox.get(3).floatValue() : 0; }

    public String getFormattedConfidence() {
        return String.format("%.1f%%", confidence * 100);
    }
}
