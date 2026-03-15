package com.droidbot.agent.brain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HybridRouter — The Switchboard.
 *
 * Routes inference requests to Edge (Gemini Nano) or Cloud (Gemini Pro)
 * based on task complexity, connectivity, and latency requirements.
 *
 * ┌────────────────────────────────────────────────┐
 * │               HybridRouter                     │
 * │                                                │
 * │  Complexity? ───► LOW ──────► Edge (Nano)      │
 * │                   MEDIUM ──► Try Edge → Cloud  │
 * │                   HIGH ────► Cloud (Pro)       │
 * │                                                │
 * │  Offline? ─────► Always Edge                   │
 * │  Edge fails? ──► Fallback to Cloud             │
 * └────────────────────────────────────────────────┘
 */
@Singleton
class HybridRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edgeInference: EdgeInference,
    private val cloudInference: CloudInference
) {

    companion object {
        private const val TAG = "HybridRouter"
    }

    /**
     * Route an inference request to the appropriate engine.
     *
     * @param prompt The prompt to process
     * @param complexity Task complexity hint
     * @return Model response text
     */
    suspend fun infer(prompt: String, complexity: TaskComplexity): String {
        return when {
            // HIGH complexity → always Cloud
            complexity == TaskComplexity.HIGH -> {
                Log.d(TAG, "Routing to Cloud (HIGH complexity)")
                cloudInference.infer(prompt)
            }

            // No connectivity → must use Edge
            !isOnline() -> {
                Log.d(TAG, "Routing to Edge (offline)")
                tryEdgeWithFallback(prompt)
            }

            // LOW complexity → prefer Edge
            complexity == TaskComplexity.LOW && edgeInference.isAvailable -> {
                Log.d(TAG, "Routing to Edge (LOW complexity)")
                tryEdgeWithFallback(prompt)
            }

            // MEDIUM → try Edge, fall back to Cloud
            complexity == TaskComplexity.MEDIUM && edgeInference.isAvailable -> {
                Log.d(TAG, "Routing to Edge-first (MEDIUM complexity)")
                tryEdgeWithFallback(prompt)
            }

            // Default → Cloud
            else -> {
                Log.d(TAG, "Routing to Cloud (default)")
                cloudInference.infer(prompt)
            }
        }
    }

    /**
     * Try Edge first, fall back to Cloud if Edge fails.
     */
    private suspend fun tryEdgeWithFallback(prompt: String): String {
        return try {
            edgeInference.infer(prompt)
        } catch (e: Exception) {
            Log.w(TAG, "Edge inference failed, falling back to Cloud", e)
            if (isOnline()) {
                cloudInference.infer(prompt)
            } else {
                throw IllegalStateException("Both Edge and Cloud inference unavailable", e)
            }
        }
    }

    /**
     * Check if the device has internet connectivity.
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
