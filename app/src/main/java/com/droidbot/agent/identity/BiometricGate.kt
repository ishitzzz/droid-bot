package com.droidbot.agent.identity

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * BiometricGate — Fingerprint Handshake.
 *
 * Wraps [BiometricPrompt] to provide a coroutine-based biometric
 * authentication gate. Used by:
 * 1. [IdentityVault] — to unlock credentials
 * 2. [PravaPaymentsBridge] — to authorize payments at the final screen
 *
 * The DroidBot agent autonomously navigates to the payment screen,
 * then **halts** and presents the biometric prompt. Only after the
 * human owner confirms via fingerprint does the final action execute.
 *
 * This is the "Human-in-the-Loop" safety checkpoint.
 */
@Singleton
class BiometricGate @Inject constructor() {

    companion object {
        private const val TAG = "BiometricGate"
    }

    /**
     * Check if biometric authentication is available on this device.
     */
    fun isAvailable(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "No biometric hardware available")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric hardware unavailable")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No biometrics enrolled")
                false
            }
            else -> false
        }
    }

    /**
     * Request biometric authentication.
     *
     * @param activity The hosting activity
     * @param title Title shown in the biometric prompt
     * @param subtitle Subtitle shown in the biometric prompt
     * @return true if authentication was successful
     * @throws BiometricAuthException if authentication failed or was cancelled
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "DroidBot Authorization",
        subtitle: String = "Confirm your identity to proceed"
    ): Boolean = suspendCancellableCoroutine { cont ->

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.i(TAG, "✅ Biometric authentication succeeded")
                if (cont.isActive) cont.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.e(TAG, "❌ Biometric auth error ($errorCode): $errString")
                if (cont.isActive) {
                    cont.resumeWithException(
                        BiometricAuthException("Authentication error: $errString", errorCode)
                    )
                }
            }

            override fun onAuthenticationFailed() {
                Log.w(TAG, "⚠️ Biometric authentication failed (bad fingerprint)")
                // Don't resume — BiometricPrompt allows retries automatically
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        // Run on main thread
        activity.runOnUiThread {
            biometricPrompt.authenticate(promptInfo)
        }

        cont.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }
}

/**
 * Exception thrown when biometric authentication fails.
 */
class BiometricAuthException(
    message: String,
    val errorCode: Int
) : Exception(message)
