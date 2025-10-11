# Compose Sample - LibVLC Video Player

A modern Android video player built with Jetpack Compose and LibVLC, demonstrating how to integrate VLC's powerful media playback capabilities in a Compose-based application.

## Screenshots

The app features a clean, Material Design 3 interface with three playback options and a full-screen video player.

## Features

### Video Playback Options

1. **Network Video Streaming**
   - Play videos from any HTTP/HTTPS URL
   - Support for HLS, RTSP, and other streaming protocols
   - Pre-configured with Apple's Bipbop HLS sample stream
   - Easy URL input with TextField

2. **Asset Video Playback**
   - Play bundled video files from app assets
   - Includes "Big Buck Bunny" sample video
   - No network or storage permissions required

3. **Device Video Selection**
   - Browse and select videos from device storage
   - Integration with Android's photo picker
   - Supports all video formats that LibVLC can decode

### Player Features

- Full-screen video playback
- Simple stop control
- Automatic resource cleanup
- Orientation change support
- Black background for cinematic experience

## Technical Implementation

### Architecture

- **Jetpack Compose** - Modern declarative UI framework
- **Material Design 3** - Latest Material theming system
- **LibVLC 4.0** - VLC media player library
- **Kotlin** - 100% Kotlin codebase
- **State Management** - Compose state with `remember` and `mutableStateOf`

### Key Components

#### MainActivity.kt
- Main activity extending `ComponentActivity`
- Permission handling for storage access
- Compose theme setup

#### MainScreen Composable
- Central UI logic and state management
- Video URI state tracking
- Play/stop state management
- Activity result launcher for video selection

#### VideoPlayerView Composable
- LibVLC integration with `VLCVideoLayout`
- Media player lifecycle management
- Automatic play/stop based on URI changes
- Resource cleanup with `DisposableEffect`

#### MainMenuView Composable
- Network video URL input
- Three playback option buttons
- Material Design 3 styling

### State Management

```kotlin
var currentVideoUri by remember { mutableStateOf<Uri?>(null) }
var isPlaying by remember { mutableStateOf(false) }
```

The app uses Compose's state management system to reactively update the UI when playback state changes.

### LibVLC Integration

#### Initialization
```kotlin
val libVLC = remember {
    LibVLC(context, arrayListOf("-vvv"))
}

val mediaPlayer = remember {
    MediaPlayer(libVLC)
}
```

#### Video Rendering
```kotlin
AndroidView(
    factory = { ctx ->
        VLCVideoLayout(ctx).apply {
            mediaPlayer.attachViews(this, null, false, false)
        }
    },
    modifier = modifier.fillMaxSize()
)
```

#### Media Loading
```kotlin
DisposableEffect(videoUri) {
    val media = when {
        videoUri == null -> Media(libVLC, context.assets.openFd("bbb.m4v"))
        else -> Media(libVLC, videoUri)
    }

    media?.let { m ->
        m.addOption(":video-title-display")
        m.addOption(":no-drop-late-frames")
        m.addOption(":no-skip-frames")
        m.addOption(":rtsp-tcp")
        mediaPlayer.media = m
        m.release()
    }

    mediaPlayer.play()

    onDispose {
        mediaPlayer.stop()
    }
}
```

## Requirements

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36

### Dependencies

```gradle
// Jetpack Compose
implementation platform('androidx.compose:compose-bom:2023.01.00')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.material3:material3'
implementation 'androidx.compose.ui:ui-tooling-preview'
implementation 'androidx.activity:activity-compose:1.6.1'

// LibVLC
implementation project(':libvlc')
```

## Permissions

The app requires the following permissions:

```xml
<!-- For network video streaming -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Storage permissions for different Android versions -->
<!-- Android 12 and below -->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- Android 13 and above -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- Android 14 and above: Partial access to photos and videos -->
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
```

### Privacy-First Approach

Starting with Android 14 (API 34), the app uses the new **Photo Picker** API:
- ✅ **No storage permissions required** when using Photo Picker
- ✅ **User selects specific videos** - app only gets access to selected files
- ✅ **Better privacy** - no broad storage access needed
- ✅ **Automatic fallback** to legacy picker on Android 12 and below

## Building and Running

### Build the APK

```bash
# Debug build
./gradlew :compose_sample:assembleDebug

# Release build
./gradlew :compose_sample:assembleRelease
```

### Install on Device

```bash
# Using Gradle
./gradlew :compose_sample:installDebug

# Using ADB
adb install compose_sample/build/outputs/apk/debug/compose_sample-debug.apk
```

### Run from Android Studio

1. Open the project in Android Studio
2. Select `compose_sample` run configuration
3. Click Run or press `Shift+F10`

## Usage Guide

### Playing Network Videos

1. Launch the app
2. Default URL is pre-filled with Apple's HLS sample
3. Edit the URL field to enter your own video URL
4. Click "Play Network Video" button
5. Video will start playing in full-screen

Supported protocols: HTTP, HTTPS, HLS, RTSP, RTMP, MMS, and more.

### Playing Sample Video

1. Launch the app
2. Click "Play Sample Video (Asset)" button
3. The bundled "Big Buck Bunny" video will play

### Playing Device Videos

1. Launch the app
2. Click "Select Video from Device" button
3. **Android 14+**: Photo Picker opens automatically (no permissions needed!)
4. **Android 13 and below**: Grant storage permission if prompted, then select video
5. Select a video from the picker
6. Video will start playing

**Note**: On Android 14 and above, the app uses the new Photo Picker which doesn't require any storage permissions. The app only gets access to the specific videos you select.

### Stopping Playback

- Click the "Stop Video" button at the bottom of the screen
- This returns to the main menu

## Supported Video Formats

Thanks to LibVLC, the app supports a wide range of formats:

### Codecs
- **Video**: H.264, H.265/HEVC, VP8, VP9, MPEG-1/2/4, AV1, and more
- **Audio**: AAC, MP3, Opus, Vorbis, FLAC, and more

### Containers
- MP4, MKV, AVI, WebM, FLV, MOV, and more

### Streaming
- HLS (m3u8)
- RTSP
- RTMP
- HTTP/HTTPS progressive download

## Troubleshooting

### Video doesn't play

1. **Check logs**: `adb logcat -s "VLC:*" "libvlc:*" "MainActivity:*"`
2. **Verify URL**: Make sure the video URL is accessible
3. **Check network**: Ensure device has internet connectivity
4. **Permissions**: Verify storage permissions are granted for local videos

### Build issues

1. **Clean build**: `./gradlew clean`
2. **Sync Gradle**: File → Sync Project with Gradle Files
3. **Invalidate caches**: File → Invalidate Caches / Restart

### App crashes on startup

1. **Check manifest**: Ensure package name matches (`org.videolan.composesample`)
2. **Reinstall**: Uninstall old version first
   ```bash
   adb uninstall org.videolan.composesample
   adb install compose_sample/build/outputs/apk/debug/compose_sample-debug.apk
   ```

## Code Structure

```
compose_sample/
├── src/main/
│   ├── java/org/videolan/composesample/
│   │   ├── MainActivity.kt          # Entry point
│   │   ├── VideoPlayerView.kt       # Video player composable
│   │   └── ui/theme/                # Material Design 3 theme
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── res/                         # Resources
│   └── AndroidManifest.xml
├── build.gradle                     # Module build config
└── README.md                        # This file
```

## Future Enhancements

Potential features to add:

- [ ] Playback controls (play/pause, seek, volume)
- [ ] Playlist support
- [ ] Subtitle support
- [ ] Audio track selection
- [ ] Picture-in-Picture mode
- [ ] Background playback
- [ ] Video download/caching
- [ ] Playback speed control
- [ ] Video quality selection

## Credits

- **LibVLC**: VLC media player library by VideoLAN
- **Sample Video**: "Big Buck Bunny" by Blender Foundation (CC BY 3.0)
- **Test Stream**: Apple's Bipbop HLS sample

## License

Copyright © 2025 VideoLAN. Licensed under BSD license.

## Resources

- [LibVLC Android Documentation](https://wiki.videolan.org/AndroidCompile)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [VLC for Android](https://www.videolan.org/vlc/download-android.html)
