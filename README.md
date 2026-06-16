# AI Fit Tracker (Android PoC) - How to Run

This project is a Proof of Concept for real-time workout tracking.

## Prerequisites
- **Android Studio** (Hedgehog or newer recommended).
- **Physical Android Device** (API 24+) - CameraX and ML Kit work best on real hardware.
- **Internet Connection** (to download Gradle and dependencies on first build).

## How to Build & Run
1.  **Open Android Studio.**
2.  Select **"Open"** and navigate to the directory: `/home/longnhat/Obsidian Vault/01 Projects/AI-Fit-Tracker`.
3.  Wait for Gradle to sync. Android Studio will automatically download the required SDKs and dependencies.
4.  Connect your Android device via USB (ensure USB Debugging is enabled).
5.  Click the **"Run"** button (Green Arrow).

## Features in this PoC
- **Real-time Pose Tracking:** Uses Google ML Kit to identify 33 body landmarks.
- **Skeleton Overlay:** Draws your "bone structure" over the camera feed.
- **Squat Counter:** 
    -   Automatically detects squats based on knee angle.
    -   Counts reps only when you reach sufficient depth (< 95 degrees).
    -   Provides real-time feedback (e.g., "Good depth", "Go deeper").
- **Privacy First:** All processing is done on-device. No video data is sent to any server.

## Troubleshooting
- **Camera Permission:** If the camera doesn't start, ensure you've granted the camera permission when prompted.
- **Performance:** If the overlay feels laggy, ensure your device isn't in power-saving mode. The app uses `STREAM_MODE` for the best real-time experience.
# AI-Fit-Tracker
# AI-Fit-Tracker
# AI-Fit-Tracker
# AI-Fit-Tracker
# AI-Fit-Tracker
