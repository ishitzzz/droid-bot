package com.droidbot.agent.navigation

import android.graphics.Bitmap
import android.util.Log
import com.droidbot.agent.brain.CloudInference
import com.droidbot.agent.service.DroidBotService
import com.droidbot.agent.service.ScreenCaptureService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SelfHealingEngine — Vision Fallback.
 *
 * When the [UITreeParser] returns an unreadable snapshot (e.g., games,
 * custom-rendered UIs, WebViews), this engine kicks in:
 *
 * 1. Captures a screenshot via [ScreenCaptureService]
 * 2. Sends the bitmap to Gemini Vision (Cloud) with the task context
 * 3. Receives (x, y) coordinates for the target element
 * 4. Dispatches a tap gesture via [DroidBotService]
 *
 * This is the "Thinking" model fallback — slower but more robust.
 */
@Singleton
class SelfHealingEngine @Inject constructor(
    private val cloudInference: CloudInference
) {

    companion object {
        private const val TAG = "SelfHealingEngine"
    }

    /**
     * Attempt to visually locate and tap the target described by [targetDescription].
     *
     * @param targetDescription Natural language description of what to tap
     *   (e.g., "the blue 'Book Now' button at the bottom of the screen")
     * @param taskContext Additional context about the current task
     * @return [ActionResult] with success/failure
     */
    suspend fun visualTap(
        targetDescription: String,
        taskContext: String = ""
    ): ActionResult {
        Log.i(TAG, "🔬 Self-healing triggered for: \"$targetDescription\"")

        // Step 1: Capture screenshot
        val captureService = ScreenCaptureService.instance
            ?: return ActionResult(false, "ScreenCaptureService not running")

        val screenshot: Bitmap
        try {
            screenshot = captureService.captureScreen()
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot capture failed", e)
            return ActionResult(false, "Screenshot capture failed: ${e.message}")
        }

        // Step 2: Send to Gemini Vision for coordinate identification
        val coordinates = try {
            cloudInference.identifyCoordinates(screenshot, targetDescription, taskContext)
        } catch (e: Exception) {
            Log.e(TAG, "Vision inference failed", e)
            screenshot.recycle()
            return ActionResult(false, "Vision inference failed: ${e.message}")
        }

        screenshot.recycle()

        if (coordinates == null) {
            return ActionResult(false, "Could not identify target: \"$targetDescription\"")
        }

        // Step 3: Dispatch tap at identified coordinates
        val service = DroidBotService.instance
            ?: return ActionResult(false, "DroidBotService not connected")

        service.dispatchTap(coordinates.first, coordinates.second)
        Log.i(TAG, "✅ Self-healed tap at (${coordinates.first}, ${coordinates.second})")

        return ActionResult(
            success = true,
            description = "Vision tap at (${coordinates.first}, ${coordinates.second}) for: \"$targetDescription\""
        )
    }

    /**
     * Describe the current screen using vision.
     * Useful when the UI tree is completely empty and we need the LLM
     * to understand what's on screen.
     */
    suspend fun describeScreen(): String? {
        val captureService = ScreenCaptureService.instance ?: return null

        return try {
            val screenshot = captureService.captureScreen()
            val description = cloudInference.describeScreenshot(screenshot)
            screenshot.recycle()
            description
        } catch (e: Exception) {
            Log.e(TAG, "Screen description failed", e)
            null
        }
    }
}
