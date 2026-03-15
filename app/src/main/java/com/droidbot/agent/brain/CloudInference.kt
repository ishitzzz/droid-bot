package com.droidbot.agent.brain

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.droidbot.agent.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton
import java.io.ByteArrayOutputStream

/**
 * CloudInference — Gemini 3 Pro (Cloud).
 *
 * Handles System 2 "thinking" tasks via the Generative AI SDK:
 * - Complex multi-step reasoning (travel booking, price comparison)
 * - Vision fallback (screenshot → element identification)
 * - Long-context planning with conversation history
 *
 * ┌────────────────────────────────────────────┐
 * │  Gemini 3 Pro (Cloud)                      │
 * │  ┌──────────────────────────────────────┐  │
 * │  │  Multi-Step Reasoning                │  │
 * │  │  Vision (Screenshot → Coordinates)   │  │
 * │  │  Context-Aware Planning              │  │
 * │  └──────────────────────────────────────┘  │
 * └────────────────────────────────────────────┘
 */
@Singleton
class CloudInference @Inject constructor() {

    companion object {
        private const val TAG = "CloudInference"
        private const val PRIMARY_MODEL = "gemini-2.5-flash"
        private const val FALLBACK_MODEL = "gemini-2.5-flash-lite"
    }

    private fun createModel(name: String, temp: Float) = GenerativeModel(
        modelName = name,
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = temp
            topP = 0.9f
            maxOutputTokens = 1024
        }
    )

    private val primaryTextModel by lazy { createModel(PRIMARY_MODEL, 0.2f) }
    private val fallbackTextModel by lazy { createModel(FALLBACK_MODEL, 0.2f) }
    
    private val primaryVisionModel by lazy { createModel(PRIMARY_MODEL, 0.1f) }
    private val fallbackVisionModel by lazy { createModel(FALLBACK_MODEL, 0.1f) }

    // ═══════════════════════════════════════════════════
    // Text Inference
    // ═══════════════════════════════════════════════════

    /**
     * Run text inference on Gemini Pro for complex reasoning.
     *
     * @param prompt The full prompt including UI state and task context
     * @return The model's response text
     */
    suspend fun infer(prompt: String): String {
        Log.d(TAG, "Cloud inference request (${prompt.length} chars)")

        return try {
            val response = primaryTextModel.generateContent(content { text(prompt) })
            response.text ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Primary model failed ($PRIMARY_MODEL), trying fallback ($FALLBACK_MODEL)", e)
            try {
                val response = fallbackTextModel.generateContent(content { text(prompt) })
                response.text ?: ""
            } catch (e2: Exception) {
                Log.e(TAG, "All models failed", e2)
                throw e2
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // Vision Inference (Self-Healing)
    // ═══════════════════════════════════════════════════

    /**
     * Identify the (x, y) coordinates of a target element in a screenshot.
     * Used by [SelfHealingEngine] when the accessibility tree is unreadable.
     *
     * @param screenshot The captured screen bitmap
     * @param targetDescription What the agent is looking for
     * @param taskContext Additional context about the current task
     * @return (x, y) coordinates or null if not found
     */
    suspend fun identifyCoordinates(
        screenshot: Bitmap,
        targetDescription: String,
        taskContext: String
    ): Pair<Float, Float>? {
        val prompt = buildString {
            appendLine("You are analyzing a phone screenshot to find a specific UI element.")
            appendLine()
            appendLine("TARGET: $targetDescription")
            if (taskContext.isNotBlank()) {
                appendLine("CONTEXT: $taskContext")
            }
            appendLine()
            appendLine("The screenshot is ${screenshot.width}x${screenshot.height} pixels.")
            appendLine("Return ONLY the pixel coordinates of the center of the target element.")
            appendLine("Format: {\"x\": 123, \"y\": 456}")
            appendLine("If the element is not visible, return: {\"x\": -1, \"y\": -1}")
        }

        return try {
            val response = primaryVisionModel.generateContent(content {
                image(screenshot)
                text(prompt)
            })
            parseCoordinates(response.text ?: "")
        } catch (e: Exception) {
            Log.w(TAG, "Primary vision failed, trying fallback", e)
            val response = fallbackVisionModel.generateContent(content {
                image(screenshot)
                text(prompt)
            })
            parseCoordinates(response.text ?: "")
        }
    }

    /**
     * Describe what's currently visible on screen.
     * Used when the UI tree is completely empty.
     */
    suspend fun describeScreenshot(screenshot: Bitmap): String {
        val prompt = buildString {
            appendLine("Describe what you see on this Android phone screenshot.")
            appendLine("Focus on: which app is open, what screen/page is shown, key UI elements, any buttons or input fields.")
            appendLine("Be concise — max 3 sentences.")
        }

        return try {
            val response = primaryVisionModel.generateContent(content {
                image(screenshot)
                text(prompt)
            })
            response.text ?: "Unable to describe screenshot"
        } catch (e: Exception) {
            Log.w(TAG, "Primary description failed, trying fallback", e)
            val response = fallbackVisionModel.generateContent(content {
                image(screenshot)
                text(prompt)
            })
            response.text ?: "Unable to describe screenshot"
        }
    }

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    private fun parseCoordinates(response: String): Pair<Float, Float>? {
        val xRegex = """"x"\s*:\s*(-?\d+\.?\d*)""".toRegex()
        val yRegex = """"y"\s*:\s*(-?\d+\.?\d*)""".toRegex()

        val x = xRegex.find(response)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        val y = yRegex.find(response)?.groupValues?.get(1)?.toFloatOrNull() ?: return null

        if (x < 0 || y < 0) return null // Element not found

        return Pair(x, y)
    }
}
