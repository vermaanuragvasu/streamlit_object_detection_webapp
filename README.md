# Object Detection App

AI-powered object detection application using **FasterRCNN ResNet50 FPN V2** (PyTorch), with a **FastAPI** backend and a **native Android** client.

## Features

### Image Detection
- Upload image from gallery or capture via camera
- Real-time object detection with bounding boxes
- Labels with confidence scores (80+ COCO categories)
- Color-coded boxes (red for person, green for other objects)

### Video Detection
- Upload video or record from camera
- Frame-by-frame object detection
- Bounding box overlay on processed video
- Export processed video with annotations
- Real-time processing FPS tracking

### Live Camera Detection
- Real-time CameraX streaming inference
- Bounding box overlay on live camera feed
- FPS and detection count HUD

### Backend API
- FastAPI REST endpoints for image and video detection
- Model loaded once at startup for optimal performance
- JSON responses with detection details
- Processed video download/streaming
- Health check endpoint
- CORS enabled for cross-origin access

## Architecture

```
Android App (Java)  --HTTP-->  FastAPI Backend (Python)
   |-- CameraX                    |-- FasterRCNN ResNet50
   |-- Retrofit                   |-- Image endpoint
   |-- ExoPlayer                  |-- Video endpoint
   |-- Material UI                +-- Health check
   +-- BoundingBoxOverlay
```

## Quick Start

### Backend
```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### Android
1. Open `android/ObjectDetectorApp/` in Android Studio
2. Sync Gradle dependencies
3. Run on device/emulator

## Documentation

- [Deployment Guide](docs/DEPLOYMENT.md) - Local, Docker, and cloud deployment
- [Play Store Publishing](docs/PLAY_STORE_PUBLISHING.md) - Step-by-step Play Store submission

## Model

- **Architecture**: FasterRCNN with ResNet50 FPN V2 backbone
- **Dataset**: COCO (80+ object categories)
- **Threshold**: 0.5 confidence score
- **Categories**: person, bicycle, car, motorcycle, airplane, bus, train, truck, boat, traffic light, fire hydrant, stop sign, and many more

## Tech Stack

| Component | Technology |
|-----------|-----------|
| ML Model | PyTorch FasterRCNN ResNet50 FPN V2 |
| Backend | FastAPI, Uvicorn, OpenCV |
| Android | Java, Material Design, CameraX, Retrofit, ExoPlayer |
| Networking | OkHttp, Gson |

## Original Streamlit App

The original Streamlit-based web app is in `streamlit_object_detection_webapp/`. Run it with:
```bash
cd streamlit_object_detection_webapp
streamlit run object_detection_app.py
```
