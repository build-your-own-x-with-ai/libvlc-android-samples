/*****************************************************************************
 * VideoPlayerView.kt
 *****************************************************************************
 * Copyright (C) 2025 VideoLAN
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the BSD license. See the LICENSE file for details.
 *****************************************************************************/

package org.videolan.composesample

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.FileDescriptor
import java.io.IOException

@Composable
fun VideoPlayerView(
    libVLC: LibVLC,
    mediaPlayer: MediaPlayer,
    videoUri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            VLCVideoLayout(ctx).apply {
                mediaPlayer.attachViews(this, null, false, false)
            }
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(videoUri) {
        val media = if (videoUri == null) {
            // Play default asset video
            try {
                val assetFileDescriptor: AssetFileDescriptor = context.assets.openFd("bbb.m4v")
                Media(libVLC, assetFileDescriptor)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else {
            // Check if it's a content URI (from Photo Picker or other content providers)
            if (videoUri.scheme == "content") {
                try {
                    // Open file descriptor for content URI
                    val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(videoUri, "r")
                    pfd?.let {
                        Media(libVLC, it.fileDescriptor)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                // For other URIs (http, https, rtsp, etc.), use URI directly
                Media(libVLC, videoUri)
            }
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
}
