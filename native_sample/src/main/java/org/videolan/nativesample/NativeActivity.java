/*****************************************************************************
 * NativeActivity.java
 *****************************************************************************
 * Copyright (C) 2016-2019 VideoLAN
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the BSD license. See the LICENSE file for details.
 *****************************************************************************/

package org.videolan.nativesample;

import android.content.Context;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.AWindow;
import org.videolan.libvlc.interfaces.IVLCVout;
import org.videolan.native_sample.R;

@SuppressWarnings("JniMissingFunction")
public class NativeActivity extends AppCompatActivity implements IVLCVout.Callback {
    private static final String TAG = "NativeActivity";

    private boolean mNativeStarted = false;
    private AWindow mAWindow = null;
    private SurfaceView mUiSurface = null;
    private SurfaceView mVideoSurface = null;
    private SurfaceView mSubtitlesSurface = null;

    private View.OnLayoutChangeListener mOnLayoutChangeListener = null;

    private static int sInit = -1;
    static synchronized boolean loadLibraries(Context context) {
        if (sInit != -1) return sInit == 1;
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("vlc");
            System.loadLibrary("vlcjni");
            System.loadLibrary("native");
        } catch (UnsatisfiedLinkError ule) {
            Toast.makeText(context, "Can't load vlcjni library: " + ule, Toast.LENGTH_LONG).show();
            sInit = 0;
            return false;
        } catch (SecurityException se) {
            Toast.makeText(context, "Encountered a security issue when loading vlcjni library: " + se, Toast.LENGTH_LONG).show();
            sInit = 0;
            return false;
        }
        sInit = 1;
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!loadLibraries(this)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        mAWindow = new AWindow(null);
        mAWindow.addCallback(this);

        mUiSurface = findViewById(R.id.ui_surface);
        mVideoSurface = findViewById(R.id.video_surface);

        if (!nativeCreate()) {
            Toast.makeText(this, "Couldn't create LibVLC", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nativeDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mAWindow.setVideoView(mVideoSurface);
        if (mSubtitlesSurface != null)
            mAWindow.setSubtitlesView(mSubtitlesSurface);
        mAWindow.attachViews();

        if (mOnLayoutChangeListener == null) {
            mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                        mAWindow.setWindowSize(right - left, bottom - top);
                    }
                }
            };
        }
        mVideoSurface.addOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    protected void onStop() {
        super.onStop();

        if (mOnLayoutChangeListener != null) {
            mVideoSurface.removeOnLayoutChangeListener(mOnLayoutChangeListener);
            mOnLayoutChangeListener = null;
        }

        if (mNativeStarted)
            nativeStop();

        mAWindow.detachViews();
    }

    public void onSurfacesCreated(IVLCVout vlcVout) {
        if (!mNativeStarted) {
            if (!nativeStart(mAWindow)) {
                Toast.makeText(this, "Couldn't start LibVLC", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        mNativeStarted = true;
    }

    public void onSurfacesDestroyed(IVLCVout vlcVout) {
        if (mNativeStarted)
            nativeStop();
    }

    @SuppressWarnings("unused") // used by JNI
    private long mInstance;
    private native boolean nativeCreate();
    private native void nativeDestroy();
    private native boolean nativeStart(AWindow aWindowNative);
    private native void nativeStop();
}
