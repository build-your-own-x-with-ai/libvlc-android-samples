/*****************************************************************************
 * MainActivity.kt
 *****************************************************************************
 * Copyright (C) 2025 VideoLAN
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the BSD license. See the LICENSE file for details.
 *****************************************************************************/

package org.videolan.composesample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.videolan.composesample.ui.theme.ComposeSampleTheme
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var currentVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Photo Picker for selecting videos (Android 13+)
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            currentVideoUri = it
            isPlaying = true
        }
    }

    // Fallback for older Android versions
    val selectVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            result.data?.data?.also { uri ->
                currentVideoUri = uri
                isPlaying = true
            }
        }
    }

    // Initialize LibVLC and MediaPlayer
    val libVLC = remember {
        LibVLC(context, arrayListOf("-vvv"))
    }

    val mediaPlayer = remember {
        MediaPlayer(libVLC)
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
            libVLC.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isPlaying) {
            // Video player view
            VideoPlayerView(
                libVLC = libVLC,
                mediaPlayer = mediaPlayer,
                videoUri = currentVideoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Player controls
            PlayerControls(
                onStopClick = {
                    isPlaying = false
                    mediaPlayer.stop()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        } else {
            // Main menu view
            MainMenuView(
                onPlayNetworkVideo = { uri ->
                    Log.d("MainActivity", "onPlayNetworkVideo called with: $uri")
                    currentVideoUri = Uri.parse(uri)
                    isPlaying = true
                },
                onPlayAssetVideo = {
                    Log.d("MainActivity", "onPlayAssetVideo called")
                    currentVideoUri = null // Use default asset
                    isPlaying = true
                },
                onSelectDeviceVideo = {
                    Log.d("MainActivity", "onSelectDeviceVideo called")
                    // Use new Photo Picker on Android 13+ (API 33+)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        pickVideoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    } else {
                        // Fallback to old picker for Android 12 and below
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        selectVideoLauncher.launch(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuView(
    onPlayNetworkVideo: (String) -> Unit,
    onPlayAssetVideo: () -> Unit,
    onSelectDeviceVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var networkUrl by remember { mutableStateOf("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8") }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "VLC Video Player",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Network video input
        Text(
            text = "Play Network Video",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = networkUrl,
            onValueChange = { networkUrl = it },
            placeholder = {
                Text("Enter video URL", color = Color.Gray)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
            singleLine = true
        )
        
        Button(
            onClick = {
                if (networkUrl.isNotBlank()) {
                    onPlayNetworkVideo(networkUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play Network Video")
        }
        
        // Asset video button
        VideoOptionButton(
            text = "Play Sample Video (Asset)",
            onClick = onPlayAssetVideo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Device video button
        VideoOptionButton(
            text = "Select Video from Device",
            onClick = onSelectDeviceVideo,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun VideoOptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D2D2D))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
fun PlayerControls(
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onStopClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Stop Video")
        }
    }
}
