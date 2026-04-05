# Google Play Store Publishing Guide

## Prerequisites

1. **Google Play Developer Account** — $25 one-time registration fee
   - Sign up at: https://play.google.com/console
2. **Signed release APK or AAB** (Android App Bundle)
3. **App assets** (icons, screenshots, descriptions)

---

## Step 1: Generate a Signing Key

```bash
keytool -genkey -v -keystore object-detector-release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias object-detector

# Store the keystore file securely — you need it for all future updates!
```

## Step 2: Configure Signing in Gradle

Add to `android/ObjectDetectorApp/app/build.gradle`:

```groovy
android {
    signingConfigs {
        release {
            storeFile file('path/to/object-detector-release.jks')
            storePassword 'YOUR_STORE_PASSWORD'
            keyAlias 'object-detector'
            keyPassword 'YOUR_KEY_PASSWORD'
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),
                         'proguard-rules.pro'
        }
    }
}
```

> **Security Tip**: Use environment variables or `local.properties` for passwords. Never commit passwords to git.

## Step 3: Build Release AAB

In Android Studio:
```
Build → Generate Signed Bundle / APK → Android App Bundle → Select keystore → Release → Finish
```

Or via command line:
```bash
cd android/ObjectDetectorApp
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Step 4: Update Production API URL

Before building the release, update `app/build.gradle`:

```groovy
buildConfigField "String", "API_BASE_URL", "\"https://your-production-api.com\""
```

## Step 5: Create Play Console Listing

1. Go to [Google Play Console](https://play.google.com/console)
2. Click **Create app**
3. Fill in:
   - **App name**: Object Detector
   - **Default language**: English (US)
   - **App or game**: App
   - **Free or paid**: Free (or Paid)

## Step 6: Store Listing

### Required Assets

| Asset | Specification |
|-------|--------------|
| App icon | 512 x 512 px, PNG, 32-bit |
| Feature graphic | 1024 x 500 px |
| Phone screenshots | Min 2, max 8 (16:9 or 9:16) |
| Tablet screenshots | Optional but recommended |

### App Description

**Short description** (80 chars max):
```
AI-powered object detection — detect 80+ objects in images and videos.
```

**Full description** (4000 chars max):
```
Object Detector uses state-of-the-art FasterRCNN ResNet50 deep learning model
to detect and identify objects in images and videos.

FEATURES:
- Image Detection: Upload or capture photos to detect objects instantly
- Video Detection: Process videos frame-by-frame with object detection
- Live Camera: Real-time object detection using your camera
- 80+ Object Categories: Detect people, vehicles, animals, and more
- Bounding Boxes: Visual annotations with labels and confidence scores
- Export: Save processed videos with detection overlays

POWERED BY AI:
Built on PyTorch FasterRCNN ResNet50 FPN V2, trained on COCO dataset
with 80+ object categories including people, vehicles, animals, and
everyday objects.
```

## Step 7: Content Rating

1. Go to **Policy → App content → Content rating**
2. Complete the IARC questionnaire
3. Select appropriate ratings (this app should be "Everyone")

## Step 8: Privacy Policy

You need a privacy policy URL. Create one that covers:
- Camera access (for photo/video capture)
- Network access (for API communication)
- No personal data collection
- Images/videos are processed and not stored permanently

Host it on a simple web page or GitHub Pages.

## Step 9: App Review Checklist

Before submitting:

- [ ] App icon and feature graphic uploaded
- [ ] At least 2 phone screenshots
- [ ] Short and full descriptions written
- [ ] Content rating completed
- [ ] Privacy policy URL added
- [ ] Target API level is 34 (latest required by Google)
- [ ] App tested on multiple screen sizes
- [ ] Production API URL configured
- [ ] Signing key backed up securely
- [ ] ProGuard rules tested (release build works correctly)

## Step 10: Release

1. Go to **Release → Production**
2. Click **Create new release**
3. Upload the `.aab` file
4. Add release notes
5. Click **Review release**
6. Click **Start rollout to Production**

### First Review Timeline
- Google typically reviews new apps within 1-7 days
- You'll receive an email when the app is approved or if changes are needed

## Step 11: Post-Launch

- Monitor **Android Vitals** for crashes and ANRs
- Respond to user reviews
- Update the app regularly
- Monitor backend server health and scaling

---

## Troubleshooting

### Common Rejection Reasons

1. **Missing privacy policy** — Always include one
2. **Misleading description** — Be accurate about features
3. **Crashes on review** — Test thoroughly on different devices
4. **Missing permissions justification** — Explain why camera/storage is needed
5. **Backend downtime** — Ensure your API is stable during review

### Tips for Approval

- Test the release build (not debug) before submitting
- Include clear screenshots showing the app in action
- Make sure the backend is deployed and accessible
- Add a content rating before submitting
- Ensure the app works offline gracefully (show error messages, not crashes)
