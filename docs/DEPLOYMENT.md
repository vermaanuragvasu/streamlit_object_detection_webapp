# Deployment Guide

## Project Structure

```
streamlit_object_detection_webapp/
├── backend/                          # FastAPI Backend
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py                   # FastAPI app, endpoints, CORS
│   │   ├── model.py                  # FasterRCNN model loading & prediction
│   │   ├── schemas.py                # Pydantic request/response models
│   │   └── video_processor.py        # Video frame-by-frame processing
│   ├── requirements.txt              # Python dependencies
│   ├── Dockerfile                    # Container build config
│   └── .env.example                  # Environment variables template
│
├── android/                          # Android App
│   └── ObjectDetectorApp/
│       ├── app/
│       │   ├── src/main/
│       │   │   ├── java/com/objectdetector/app/
│       │   │   │   ├── MainActivity.java           # Home screen
│       │   │   │   ├── ImageDetectionActivity.java # Image detection
│       │   │   │   ├── VideoDetectionActivity.java # Video detection
│       │   │   │   ├── CameraActivity.java         # Live camera detection
│       │   │   │   ├── api/
│       │   │   │   │   ├── ApiClient.java          # Retrofit singleton
│       │   │   │   │   └── ApiService.java         # API endpoints interface
│       │   │   │   ├── models/
│       │   │   │   │   ├── Detection.java          # Detection data model
│       │   │   │   │   ├── ImageDetectionResponse.java
│       │   │   │   │   └── VideoDetectionResponse.java
│       │   │   │   ├── views/
│       │   │   │   │   └── BoundingBoxOverlay.java # Custom bounding box view
│       │   │   │   └── utils/
│       │   │   │       └── FileUtils.java          # File handling utilities
│       │   │   ├── res/
│       │   │   │   ├── layout/                     # XML layouts
│       │   │   │   ├── values/                     # Colors, strings, themes
│       │   │   │   ├── drawable/                   # Status indicators, icons
│       │   │   │   └── xml/                        # Network security, file paths
│       │   │   └── AndroidManifest.xml
│       │   └── build.gradle                        # App-level dependencies
│       ├── build.gradle                            # Project-level config
│       ├── settings.gradle
│       └── gradle.properties
│
├── docs/
│   ├── DEPLOYMENT.md                 # This file
│   └── PLAY_STORE_PUBLISHING.md      # Play Store submission guide
│
└── streamlit_object_detection_webapp/ # Original Streamlit app
    └── object_detection_app.py
```

---

## Backend Deployment

### Option 1: Local Development

```bash
# Navigate to backend directory
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate   # Windows

# Install dependencies
pip install -r requirements.txt

# Run the server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The API will be available at `http://localhost:8000`. API docs at `http://localhost:8000/docs`.

### Option 2: Docker

```bash
cd backend

# Build the Docker image
docker build -t object-detector-api .

# Run the container
docker run -p 8000:8000 object-detector-api

# With GPU support (requires nvidia-docker)
docker run --gpus all -p 8000:8000 object-detector-api
```

### Option 3: Cloud Deployment (AWS EC2)

```bash
# 1. Launch an EC2 instance (recommended: g4dn.xlarge for GPU)
# 2. Install Docker
sudo yum update -y
sudo yum install docker -y
sudo service docker start

# 3. Build and run
docker build -t object-detector-api .
docker run -d -p 8000:8000 --name detector object-detector-api
```

### Option 4: Cloud Deployment (Google Cloud Run)

```bash
# 1. Build and push to Container Registry
gcloud builds submit --tag gcr.io/YOUR_PROJECT/object-detector-api

# 2. Deploy to Cloud Run
gcloud run deploy object-detector-api \
    --image gcr.io/YOUR_PROJECT/object-detector-api \
    --platform managed \
    --memory 4Gi \
    --cpu 2 \
    --timeout 300 \
    --allow-unauthenticated
```

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check & model status |
| `/detect/image` | POST | Detect objects in an image (multipart upload) |
| `/detect/video` | POST | Process video for object detection (multipart upload) |
| `/detect/video/download/{filename}` | GET | Download processed video |
| `/output/{filename}` | GET | Stream processed video |

### Testing the API

```bash
# Health check
curl http://localhost:8000/health

# Image detection
curl -X POST http://localhost:8000/detect/image \
  -F "file=@test_image.jpg"

# Video detection
curl -X POST http://localhost:8000/detect/video \
  -F "file=@test_video.mp4"
```

---

## Android App Setup

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- JDK 17
- Android SDK 34
- Android device or emulator (API 24+)

### Build Steps

1. **Open the project** in Android Studio:
   ```
   File → Open → Select android/ObjectDetectorApp/
   ```

2. **Sync Gradle** — Android Studio will automatically download dependencies.

3. **Configure the backend URL**:
   Edit `app/build.gradle`:
   ```groovy
   // For local emulator (default)
   buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:8000\""

   // For physical device on same network
   buildConfigField "String", "API_BASE_URL", "\"http://YOUR_PC_IP:8000\""

   // For production
   buildConfigField "String", "API_BASE_URL", "\"https://your-api-domain.com\""
   ```

4. **Build the APK**:
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

5. **Run on device/emulator**:
   ```
   Run → Run 'app'
   ```

### Emulator Setup

For testing with the emulator:
- `10.0.2.2` maps to your host machine's `localhost`
- Make sure the backend server is running on port 8000
- Network security config already allows cleartext for local development

### Physical Device Setup

1. Enable USB debugging on your Android device
2. Connect via USB
3. Update `API_BASE_URL` to your computer's local IP
4. Ensure both devices are on the same WiFi network

---

## Performance Notes

- **Image detection**: Typically 200-500ms on CPU, 50-100ms with GPU
- **Video processing**: ~5-15 FPS on CPU, ~30 FPS with GPU
- **Frame resize**: All video frames are resized to max 640px dimension to optimize processing
- **Batch processing**: Video frames are processed in batches of 4 for better throughput
- **Model loading**: Model is loaded once at startup and cached in memory
- **Thread safety**: Model access is thread-safe with locking
