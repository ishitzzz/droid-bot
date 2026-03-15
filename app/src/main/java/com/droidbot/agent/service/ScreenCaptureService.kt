package com.droidbot.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ScreenCaptureService — The Self-Healing Eyes.
 *
 * A foreground service that manages [MediaProjection] for taking
 * screenshots when the AccessibilityService UI tree is unreadable.
 *
 * Flow:
 * 1. [SelfHealingEngine] detects unreadable UI
 * 2. Requests a screenshot via [captureScreen]
 * 3. The bitmap is sent to Gemini Vision for (x, y) identification
 * 4. DroidBotService dispatches a gesture to those coordinates
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "droidbot_capture"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        @Volatile
        var instance: ScreenCaptureService? = null
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 1080
    private var screenHeight = 1920
    private var screenDensity = 420

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        fetchScreenMetrics()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val resultCode = it.getIntExtra(EXTRA_RESULT_CODE, -1)
            val resultData = it.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
            if (resultData != null) {
                initProjection(resultCode, resultData)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        tearDown()
        instance = null
    }

    // ═══════════════════════════════════════════════════
    // MediaProjection Setup
    // ═══════════════════════════════════════════════════

    private fun initProjection(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "DroidBotCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, null
        )

        Log.i(TAG, "📸 MediaProjection initialized ($screenWidth x $screenHeight)")
    }

    // ═══════════════════════════════════════════════════
    // Screenshot Capture
    // ═══════════════════════════════════════════════════

    /**
     * Captures the current screen as a [Bitmap].
     * Called by [SelfHealingEngine] when the UI tree is unreadable.
     */
    suspend fun captureScreen(): Bitmap = suspendCancellableCoroutine { cont ->
        val reader = imageReader
        if (reader == null) {
            cont.resumeWithException(IllegalStateException("ImageReader not initialized. Call initProjection first."))
            return@suspendCancellableCoroutine
        }

        // Acquire the latest image
        val image = reader.acquireLatestImage()
        if (image == null) {
            cont.resumeWithException(IllegalStateException("No image available from ImageReader."))
            return@suspendCancellableCoroutine
        }

        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            if (pixelStride <= 0) {
                cont.resumeWithException(IllegalStateException("Invalid pixelStride: $pixelStride"))
                return@suspendCancellableCoroutine
            }

            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop to actual screen size if padding exists
            val croppedBitmap = if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight).also {
                    bitmap.recycle()
                }
            } else {
                bitmap
            }

            cont.resume(croppedBitmap)
            Log.d(TAG, "Screenshot captured: ${screenWidth}x${screenHeight}")
        } catch(e: Exception) {
            Log.e(TAG, "Failed to capture screenshot bitmap", e)
            if (cont.isActive) {
                cont.resumeWithException(e)
            }
        } finally {
            image.close()
        }
    }

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    private fun fetchScreenMetrics() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DroidBot Screen Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing notification for screen capture permission"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("DroidBot Active")
            .setContentText("Screen capture is running for self-healing navigation.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun tearDown() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        Log.i(TAG, "MediaProjection torn down.")
    }
}
