/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import androidx.annotation.Nullable;
import android.view.Surface;

import org.telegram.messenger.FileLog;

/**
 * An implementation of VideoCapturer to capture the screen content as a video stream.
 * Capturing is done by {@code MediaProjection} on a {@code SurfaceTexture}. We interact with this
 * {@code SurfaceTexture} using a {@code SurfaceTextureHelper}.
 * The {@code SurfaceTextureHelper} is created by the native code and passed to this capturer in
 * {@code VideoCapturer.initialize()}. On receiving a new frame, this capturer passes it
 * as a texture to the native code via {@code CapturerObserver.onFrameCaptured()}. This takes
 * place on the HandlerThread of the given {@code SurfaceTextureHelper}. When done with each frame,
 * the native code returns the buffer to the  {@code SurfaceTextureHelper} to be used for new
 * frames. At any time, at most one frame is being processed.
 *
 * @note This class is only supported on Android Lollipop and above.
 */
@TargetApi(21)
public class ScreenCapturerAndroid implements VideoCapturer, VideoSink {
  private static final int DISPLAY_FLAGS =
      DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
  // DPI for VirtualDisplay, does not seem to matter for us.
  private static final int VIRTUAL_DISPLAY_DPI = 400;

  private final Intent mediaProjectionPermissionResultData;
  private final MediaProjection.Callback mediaProjectionCallback;

  private int width;
  private int height;
  @Nullable private VirtualDisplay virtualDisplay;
  // Own the producer Surface wrapper, but never the helper's shared SurfaceTexture.
  @Nullable private Surface captureSurface;
  @Nullable private SurfaceTexture captureSurfaceTexture;
  @Nullable private SurfaceTextureHelper surfaceTextureHelper;
  @Nullable private CapturerObserver capturerObserver;
  private long numCapturedFrames;
  @Nullable private MediaProjection mediaProjection;
  private boolean isDisposed;
  private boolean captureStarted;
  @Nullable private MediaProjectionManager mediaProjectionManager;

  /**
   * Constructs a new Screen Capturer.
   *
   * @param mediaProjectionPermissionResultData the result data of MediaProjection permission
   *     activity; the calling app must validate that result code is Activity.RESULT_OK before
   *     calling this method.
   * @param mediaProjectionCallback MediaProjection callback to implement application specific
   *     logic in events such as when the user revokes a previously granted capture permission.
   **/
  public ScreenCapturerAndroid(Intent mediaProjectionPermissionResultData,
                               MediaProjection.Callback mediaProjectionCallback) {
    this.mediaProjectionPermissionResultData = mediaProjectionPermissionResultData;
    this.mediaProjectionCallback = mediaProjectionCallback;
  }

  private void checkNotDisposed() {
    if (isDisposed) {
      throw new RuntimeException("capturer is disposed.");
    }
  }

  @Nullable
  public MediaProjection getMediaProjection() {
    return mediaProjection;
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void initialize(final SurfaceTextureHelper surfaceTextureHelper,
                                      final Context applicationContext, final CapturerObserver capturerObserver) {
    checkNotDisposed();

    if (capturerObserver == null) {
      throw new RuntimeException("capturerObserver not set.");
    }
    this.capturerObserver = capturerObserver;

    if (surfaceTextureHelper == null) {
      throw new RuntimeException("surfaceTextureHelper not set.");
    }
    this.surfaceTextureHelper = surfaceTextureHelper;

    mediaProjectionManager = (MediaProjectionManager) applicationContext.getSystemService(
        Context.MEDIA_PROJECTION_SERVICE);
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void startCapture(
      final int width, final int height, final int ignoredFramerate) {
    if (mediaProjection != null || mediaProjectionManager == null) {
      return;
    }
    try {
      checkNotDisposed();

      this.width = width;
      this.height = height;

      mediaProjection = mediaProjectionManager.getMediaProjection(
          Activity.RESULT_OK, mediaProjectionPermissionResultData);

      // Let MediaProjection callback use the SurfaceTextureHelper thread.
      mediaProjection.registerCallback(mediaProjectionCallback, surfaceTextureHelper.getHandler());

      createVirtualDisplay();
      captureStarted = true;
      capturerObserver.onCapturerStarted(true);
      surfaceTextureHelper.startListening(ScreenCapturerAndroid.this);
    } catch (Throwable e) {
      handleCaptureFailure(e);
    }
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void stopCapture() {
    checkNotDisposed();
    if (surfaceTextureHelper == null) {
      return;
    }
    ThreadUtils.invokeAtFrontUninterruptibly(
        surfaceTextureHelper.getHandler(), this::stopCaptureOnSurfaceThread);
  }

  private void stopCaptureOnSurfaceThread() {
    final boolean wasStarted = captureStarted;
    captureStarted = false;
    try {
      surfaceTextureHelper.stopListening();
    } finally {
      try {
        releaseVirtualDisplay();
      } finally {
        try {
          final MediaProjection projection = mediaProjection;
          mediaProjection = null;
          if (projection != null) {
            // Unregister before stopping, otherwise onStop can recursively stop the capturer.
            try {
              projection.unregisterCallback(mediaProjectionCallback);
            } finally {
              projection.stop();
            }
          }
        } finally {
          if (wasStarted) capturerObserver.onCapturerStopped();
        }
      }
    }
  }

  private void releaseVirtualDisplay() {
    final VirtualDisplay display = virtualDisplay;
    virtualDisplay = null;
    try {
      if (display != null) display.release();
    } finally {
      releaseCaptureSurface();
    }
  }

  private void releaseCaptureSurface() {
    final Surface surface = captureSurface;
    captureSurface = null;
    captureSurfaceTexture = null;
    if (surface != null) surface.release();
  }

  private void handleCaptureFailure(Throwable failure) {
    FileLog.e(failure);
    final boolean wasStarted = captureStarted;
    if (surfaceTextureHelper != null) {
      try {
        ThreadUtils.invokeAtFrontUninterruptibly(
            surfaceTextureHelper.getHandler(), this::stopCaptureOnSurfaceThread);
      } catch (Throwable cleanupFailure) {
        FileLog.e(cleanupFailure);
      }
    }
    if (!wasStarted && capturerObserver != null) capturerObserver.onCapturerStarted(false);
    mediaProjectionCallback.onStop();
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void dispose() {
    if (isDisposed) return;
    try {
      stopCapture();
    } finally {
      isDisposed = true;
    }
  }

  /**
   * Changes output video format. This method can be used to scale the output
   * video, or to change orientation when the captured screen is rotated for example.
   *
   * @param width new output video width
   * @param height new output video height
   * @param ignoredFramerate ignored
   */
  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void changeCaptureFormat(
      final int width, final int height, final int ignoredFramerate) {
    checkNotDisposed();

    this.width = width;
    this.height = height;

    if (virtualDisplay == null) {
      // Capturer is stopped, the virtual display will be created in startCaptuer().
      return;
    }

    // Create a new virtual display on the surfaceTextureHelper thread to avoid interference
    // with frame processing, which happens on the same thread (we serialize events by running
    // them on the same thread).
    ThreadUtils.invokeAtFrontUninterruptibly(surfaceTextureHelper.getHandler(), () -> {
      if (virtualDisplay == null) {
        return;
      }
      // Since Android 14 a MediaProjection allows only one createVirtualDisplay() call and
      // re-creating it stops the projection, so resize the existing display instead
      // (before Android 12 resizing can scale the captured screen incorrectly).
      try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
          releaseVirtualDisplay();
          createVirtualDisplay();
        } else {
          surfaceTextureHelper.setTextureSize(width, height);
          virtualDisplay.resize(width, height, VIRTUAL_DISPLAY_DPI);
          final SurfaceTexture texture = surfaceTextureHelper.getSurfaceTexture();
          if (captureSurface == null || captureSurfaceTexture != texture) {
            final Surface replacement = new Surface(texture);
            try {
              virtualDisplay.setSurface(replacement);
            } catch (Throwable failure) {
              replacement.release();
              throw failure;
            }
            final Surface previous = captureSurface;
            captureSurface = replacement;
            captureSurfaceTexture = texture;
            if (previous != null) previous.release();
          }
        }
      } catch (Throwable failure) {
        handleCaptureFailure(failure);
      }
    });
  }

  private void createVirtualDisplay() {
    surfaceTextureHelper.setTextureSize(width, height);
    final SurfaceTexture texture = surfaceTextureHelper.getSurfaceTexture();
    if (captureSurface == null || captureSurfaceTexture != texture) {
      releaseCaptureSurface();
      captureSurface = new Surface(texture);
      captureSurfaceTexture = texture;
    }
    virtualDisplay = mediaProjection.createVirtualDisplay("WebRTC_ScreenCapture", width, height,
        VIRTUAL_DISPLAY_DPI, DISPLAY_FLAGS, captureSurface,
        null /* callback */, null /* callback handler */);
    if (virtualDisplay == null) throw new IllegalStateException("Could not create screen capture display");
  }

  // This is called on the internal looper thread of {@Code SurfaceTextureHelper}.
  @Override
  public void onFrame(VideoFrame frame) {
    numCapturedFrames++;
    capturerObserver.onFrameCaptured(frame);
  }

  @Override
  public boolean isScreencast() {
    return true;
  }

  public long getNumCapturedFrames() {
    return numCapturedFrames;
  }
}
