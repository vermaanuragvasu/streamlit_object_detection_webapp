"""
FastAPI Backend for Object Detection

Production-ready API serving FasterRCNN ResNet50 FPN V2 model
for image and video object detection.
"""

import os
import tempfile
import time
from contextlib import asynccontextmanager

import numpy as np
import torch
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from PIL import Image

from .model import get_device, load_model, predict_image
from .schemas import (
    Detection,
    HealthResponse,
    ImageDetectionResponse,
)
from .video_processor import process_video

# Directory for processed video output
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "/tmp/detection_output")
os.makedirs(OUTPUT_DIR, exist_ok=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load model on startup."""
    load_model()
    yield


app = FastAPI(
    title="Object Detection API",
    description="Production API for object detection using FasterRCNN ResNet50 FPN V2",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS configuration for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Serve processed videos as static files
app.mount("/output", StaticFiles(directory=OUTPUT_DIR), name="output")

ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/jpg"}
ALLOWED_VIDEO_TYPES = {"video/mp4", "video/avi", "video/mov", "video/quicktime", "video/x-msvideo"}
MAX_IMAGE_SIZE = 10 * 1024 * 1024  # 10MB
MAX_VIDEO_SIZE = 100 * 1024 * 1024  # 100MB


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Check API health and model status."""
    model = load_model()
    device = get_device()
    return HealthResponse(
        status="healthy",
        model_loaded=model is not None,
        device=str(device),
    )


@app.post("/detect/image", response_model=ImageDetectionResponse)
async def detect_image(file: UploadFile = File(...)):
    """
    Detect objects in an uploaded image.

    Accepts JPEG/PNG images up to 10MB.
    Returns detected objects with labels, confidence scores, and bounding boxes.
    """
    if file.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type: {file.content_type}. Allowed: JPEG, PNG",
        )

    contents = await file.read()
    if len(contents) > MAX_IMAGE_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"File too large. Maximum size: {MAX_IMAGE_SIZE // (1024*1024)}MB",
        )

    start_time = time.time()

    try:
        img = Image.open(
            __import__("io").BytesIO(contents)
        ).convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="Could not open image file")

    img_width, img_height = img.size
    img_array = np.array(img).transpose(2, 0, 1)  # (H,W,3) -> (3,H,W)
    img_tensor = torch.tensor(img_array, dtype=torch.uint8)

    prediction = predict_image(img_tensor)

    processing_time = (time.time() - start_time) * 1000  # ms

    detections = [
        Detection(label=label, confidence=round(score, 4), bbox=box)
        for label, score, box in zip(
            prediction["labels"], prediction["scores"], prediction["boxes"]
        )
    ]

    return ImageDetectionResponse(
        detections=detections,
        image_width=img_width,
        image_height=img_height,
        processing_time_ms=round(processing_time, 2),
    )


@app.post("/detect/video")
async def detect_video(file: UploadFile = File(...)):
    """
    Detect objects in an uploaded video.

    Accepts MP4/AVI/MOV videos up to 100MB.
    Processes frame-by-frame and returns annotated video with detection results.
    """
    if file.content_type not in ALLOWED_VIDEO_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type: {file.content_type}. Allowed: MP4, AVI, MOV",
        )

    contents = await file.read()
    if len(contents) > MAX_VIDEO_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"File too large. Maximum size: {MAX_VIDEO_SIZE // (1024*1024)}MB",
        )

    # Save uploaded video to temp file
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp:
        tmp.write(contents)
        tmp_path = tmp.name

    try:
        result = process_video(tmp_path, OUTPUT_DIR)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Video processing failed: {str(e)}")
    finally:
        os.unlink(tmp_path)

    return {
        "total_frames": result["total_frames"],
        "processed_frames": result["processed_frames"],
        "fps": result["fps"],
        "processing_time_seconds": result["processing_time_seconds"],
        "output_video_url": f"/output/{result['output_filename']}",
        "frame_detections": result["frame_detections"],
    }


@app.get("/detect/video/download/{filename}")
async def download_video(filename: str):
    """Download a processed video by filename."""
    file_path = os.path.join(OUTPUT_DIR, filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Video not found")
    return FileResponse(
        file_path,
        media_type="video/mp4",
        filename=filename,
    )
