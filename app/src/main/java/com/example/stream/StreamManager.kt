package com.example.stream

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.view.OpenGlView
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.video.CameraHelper

/**
 * Singleton stream controller managing real Camera2 RTMP broadcasting,
 * hardware encoder state, and live connection verification.
 */
object StreamManager : ConnectChecker {

    var rtmpCamera: RtmpCamera2? = null
    var isReady = false
    var isLive = false
    var openGlView: OpenGlView? = null

    var currentBitrateBps: Long = 0L
    var realLatencyMs: Int = 0

    var onConnectionSuccess: (() -> Unit)? = null
    var onConnectionFailed: ((String) -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null

    fun init(context: Context) {
        try {
            if (rtmpCamera == null) {
                rtmpCamera = RtmpCamera2(context.applicationContext, this)
                isReady = true
            }
        } catch (e: Exception) {
            Log.e("StreamManager", "Error initializing RtmpCamera2 with context", e)
        }
    }

    fun init(openGlView: OpenGlView, context: Context) {
        this.openGlView = openGlView
        try {
            if (rtmpCamera == null) {
                rtmpCamera = RtmpCamera2(openGlView, this)
            } else {
                if (openGlView.holder.surface.isValid) {
                    try {
                        rtmpCamera?.replaceView(openGlView)
                    } catch (e: Exception) {
                        Log.w("StreamManager", "replaceView failed, recreating RtmpCamera2", e)
                        rtmpCamera = RtmpCamera2(openGlView, this)
                    }
                }
            }
            isReady = true
        } catch (e: Exception) {
            Log.e("StreamManager", "Error initializing RtmpCamera2", e)
        }
    }

    fun startPreview(facing: CameraHelper.Facing) {
        val camera = rtmpCamera ?: return
        val glView = openGlView ?: return
        try {
            if (!glView.holder.surface.isValid) {
                Log.d("StreamManager", "Surface not valid yet; preview deferred to surfaceChanged")
                return
            }
            if (!glView.isRunning) {
                glView.start()
                var waited = 0
                while (!glView.isRunning && waited < 200) {
                    Thread.sleep(10)
                    waited += 10
                }
            }
            if (!camera.isOnPreview) {
                camera.startPreview(facing)
            }
        } catch (e: Exception) {
            Log.e("StreamManager", "Error starting preview", e)
        }
    }

    fun stopPreview() {
        try {
            val camera = rtmpCamera ?: return
            if (camera.isOnPreview && !camera.isStreaming) {
                camera.stopPreview()
            }
        } catch (e: Exception) {
            Log.e("StreamManager", "Error stopping preview", e)
        }
    }

    fun handleSurfaceDestroyed(context: Context) {
        try {
            val camera = rtmpCamera
            if (camera != null && camera.isStreaming) {
                camera.replaceView(context.applicationContext)
            } else {
                stopPreview()
            }
        } catch (e: Exception) {
            Log.e("StreamManager", "Error handling surface destruction", e)
        }
    }

    fun startStream(
        context: Context,
        url: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        audioBitrateKbps: Int
    ) {
        try {
            if (rtmpCamera == null) {
                init(context)
            }
            val camera = rtmpCamera
            if (camera == null) {
                onConnectionFailed?.invoke("Encoder pipeline could not be initialized")
                return
            }
            if (camera.isStreaming) return

            val preparedVideo = camera.prepareVideo(width, height, fps, bitrateKbps * 1024, 0)
            val preparedAudio = camera.prepareAudio(audioBitrateKbps * 1024, 48000, true)

            if (preparedVideo && preparedAudio) {
                camera.startStream(url)
                isLive = true
            } else {
                onConnectionFailed?.invoke("Failed to prepare video or audio encoder hardware")
            }
        } catch (e: Exception) {
            Log.e("StreamManager", "Error starting stream", e)
            onConnectionFailed?.invoke(e.localizedMessage ?: "Unknown encoder error")
        }
    }

    fun startStream(url: String, width: Int, height: Int, fps: Int, bitrateKbps: Int, audioBitrateKbps: Int) {
        val camera = rtmpCamera
        if (camera == null) {
            onConnectionFailed?.invoke("Encoder camera not initialized")
            return
        }
        if (camera.isStreaming) return

        try {
            val preparedVideo = camera.prepareVideo(width, height, fps, bitrateKbps * 1024, 0)
            val preparedAudio = camera.prepareAudio(audioBitrateKbps * 1024, 48000, true)

            if (preparedVideo && preparedAudio) {
                camera.startStream(url)
                isLive = true
            } else {
                onConnectionFailed?.invoke("Failed to prepare video or audio encoder")
            }
        } catch (e: Exception) {
            Log.e("StreamManager", "Error starting stream", e)
            onConnectionFailed?.invoke(e.localizedMessage ?: "Unknown encoder error")
        }
    }

    fun stopStream() {
        try {
            if (rtmpCamera?.isStreaming == true) {
                rtmpCamera?.stopStream()
            }
        } catch (e: Exception) {
            Log.e("StreamManager", "Error stopping stream", e)
        }
        isLive = false
        currentBitrateBps = 0L
    }

    suspend fun testServerConnection(url: String): RtmpTestResult {
        return RtmpConnectionTester.testConnection(url)
    }

    override fun onConnectionStarted(url: String) {}

    override fun onConnectionSuccess() {
        Handler(Looper.getMainLooper()).post {
            onConnectionSuccess?.invoke()
        }
    }

    override fun onConnectionFailed(reason: String) {
        Handler(Looper.getMainLooper()).post {
            isLive = false
            try {
                rtmpCamera?.stopStream()
            } catch (e: Exception) {
                Log.e("StreamManager", "Error stopping stream on failure", e)
            }
            onConnectionFailed?.invoke(reason)
        }
    }

    override fun onNewBitrate(bitrate: Long) {
        currentBitrateBps = bitrate
    }

    override fun onDisconnect() {
        Handler(Looper.getMainLooper()).post {
            isLive = false
            currentBitrateBps = 0L
            onDisconnect?.invoke()
        }
    }

    override fun onAuthError() {
        Handler(Looper.getMainLooper()).post {
            onConnectionFailed?.invoke("Authentication error: Stream Key was rejected by the server")
        }
    }

    override fun onAuthSuccess() {}
}
