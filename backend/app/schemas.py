"""Pydantic schemas for API request/response models."""

from pydantic import BaseModel


class Detection(BaseModel):
    """A single detected object."""

    label: str
    confidence: float
    bbox: list[float]  # [x1, y1, x2, y2]


class ImageDetectionResponse(BaseModel):
    """Response for image detection endpoint."""

    detections: list[Detection]
    image_width: int
    image_height: int
    processing_time_ms: float


class VideoDetectionResponse(BaseModel):
    """Response for video detection endpoint."""

    total_frames: int
    processed_frames: int
    fps: float
    processing_time_seconds: float
    output_video_url: str
    frame_detections: list[dict]


class HealthResponse(BaseModel):
    """Health check response."""

    status: str
    model_loaded: bool
    device: str
