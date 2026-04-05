package com.objectdetector.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.objectdetector.app.models.Detection;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom View that draws bounding boxes and labels over an image.
 * Scales detection coordinates to match the displayed image dimensions.
 */
public class BoundingBoxOverlay extends View {
    private List<Detection> detections = new ArrayList<>();
    private int imageWidth = 1;
    private int imageHeight = 1;

    private final Paint boxPaint;
    private final Paint personBoxPaint;
    private final Paint textPaint;
    private final Paint textBackgroundPaint;

    private static final float STROKE_WIDTH = 4f;
    private static final float TEXT_SIZE = 36f;
    private static final float TEXT_PADDING = 8f;
    private static final float CORNER_RADIUS = 4f;

    public BoundingBoxOverlay(Context context) {
        this(context, null);
    }

    public BoundingBoxOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoundingBoxOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(STROKE_WIDTH);
        boxPaint.setAntiAlias(true);

        personBoxPaint = new Paint();
        personBoxPaint.setColor(Color.RED);
        personBoxPaint.setStyle(Paint.Style.STROKE);
        personBoxPaint.setStrokeWidth(STROKE_WIDTH);
        personBoxPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);

        textBackgroundPaint = new Paint();
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setAntiAlias(true);
    }

    /**
     * Set detection results and image dimensions, then trigger redraw.
     */
    public void setDetections(List<Detection> detections, int imageWidth, int imageHeight) {
        this.detections = detections != null ? detections : new ArrayList<>();
        this.imageWidth = Math.max(imageWidth, 1);
        this.imageHeight = Math.max(imageHeight, 1);
        invalidate();
    }

    /**
     * Clear all detections from the overlay.
     */
    public void clear() {
        this.detections = new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (detections.isEmpty()) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Calculate scale factors to map image coordinates to view coordinates
        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;

        // Use uniform scaling to maintain aspect ratio
        float scale = Math.min(scaleX, scaleY);
        float offsetX = (viewWidth - imageWidth * scale) / 2f;
        float offsetY = (viewHeight - imageHeight * scale) / 2f;

        for (Detection detection : detections) {
            float x1 = detection.getX1() * scale + offsetX;
            float y1 = detection.getY1() * scale + offsetY;
            float x2 = detection.getX2() * scale + offsetX;
            float y2 = detection.getY2() * scale + offsetY;

            boolean isPerson = "person".equalsIgnoreCase(detection.getLabel());
            Paint currentBoxPaint = isPerson ? personBoxPaint : boxPaint;
            int bgColor = isPerson ? Color.argb(180, 220, 20, 20) : Color.argb(180, 20, 150, 20);
            textBackgroundPaint.setColor(bgColor);

            // Draw bounding box
            RectF rect = new RectF(x1, y1, x2, y2);
            canvas.drawRect(rect, currentBoxPaint);

            // Draw label with confidence
            String label = detection.getLabel() + " " + detection.getFormattedConfidence();
            float textWidth = textPaint.measureText(label);
            float textHeight = textPaint.getTextSize();

            // Label background
            RectF labelBg = new RectF(
                    x1,
                    y1 - textHeight - TEXT_PADDING * 2,
                    x1 + textWidth + TEXT_PADDING * 2,
                    y1
            );

            // Ensure label stays within view
            if (labelBg.top < 0) {
                labelBg.offset(0, -labelBg.top);
            }

            canvas.drawRoundRect(labelBg, CORNER_RADIUS, CORNER_RADIUS, textBackgroundPaint);
            canvas.drawText(label, labelBg.left + TEXT_PADDING,
                    labelBg.bottom - TEXT_PADDING, textPaint);
        }
    }
}
