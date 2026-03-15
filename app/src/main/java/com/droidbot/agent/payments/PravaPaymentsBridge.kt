package com.droidbot.agent.payments

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.droidbot.agent.identity.BiometricGate
import com.droidbot.agent.identity.BiometricAuthException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PravaPaymentsBridge — Scoped Token Payments with Biometric Handshake.
 *
 * Every autonomous payment follows this strict flow:
 *
 * ┌──────────────────────────────────────────────┐
 * │  1. Agent navigates to payment screen        │
 * │  2. Agent fills in payment details            │
 * │  3. Agent HALTS at the final "Pay" button     │
 * │  4. → BiometricGate: Fingerprint Handshake ← │
 * │  5. If approved → Scoped Token → Confirm     │
 * │  6. If denied → Abort & notify user           │
 * └──────────────────────────────────────────────┘
 *
 * The Scoped Token ensures:
 * - A token is issued only for the exact amount
 * - The token is single-use and time-limited
 * - The human must physically approve via biometrics
 *
 * TODO: Replace stubs with actual Prava Payments SDK calls
 * when the SDK coordinates / AAR are provided.
 */
@Singleton
class PravaPaymentsBridge @Inject constructor(
    private val biometricGate: BiometricGate
) {

    companion object {
        private const val TAG = "PravaPaymentsBridge"
    }

    /**
     * Whether the Prava SDK is initialized and ready.
     */
    var isInitialized: Boolean = false
        private set

    /**
     * Initialize the Prava Payments SDK.
     * Call once during app startup.
     */
    fun initialize(apiKey: String) {
        // TODO: Initialize Prava SDK
        // PravaSdk.initialize(context, apiKey)
        isInitialized = true
        Log.i(TAG, "💳 Prava Payments SDK initialized")
    }

    /**
     * Request a scoped payment token for the given amount.
     *
     * This method:
     * 1. Presents a biometric prompt for user authorization
     * 2. If approved, creates a scoped token via the Prava SDK
     * 3. Returns the token for the agent to use at checkout
     *
     * @param activity The hosting activity (needed for BiometricPrompt)
     * @param amount Payment amount in smallest currency unit (e.g., paise)
     * @param currency Currency code (e.g., "INR", "USD")
     * @param merchantId The merchant/vendor identifier
     * @param description Human-readable description of the payment
     * @return [PaymentToken] if successful, null if denied
     */
    suspend fun requestScopedToken(
        activity: FragmentActivity,
        amount: Long,
        currency: String,
        merchantId: String,
        description: String
    ): PaymentToken? {
        Log.i(TAG, "💳 Payment request: $currency $amount to $merchantId — \"$description\"")

        // Step 1: Biometric handshake — MUST succeed before any payment
        val isAuthorized = try {
            biometricGate.authenticate(
                activity = activity,
                title = "Authorize Payment",
                subtitle = "$currency ${amount / 100.0} — $description"
            )
        } catch (e: BiometricAuthException) {
            Log.e(TAG, "❌ Payment denied — biometric auth failed", e)
            return null
        }

        if (!isAuthorized) {
            Log.w(TAG, "❌ Payment denied by user")
            return null
        }

        // Step 2: Create scoped token via Prava SDK
        // TODO: Replace with actual Prava SDK call
        // val tokenRequest = PravaSdk.TokenRequest(
        //     amount = amount,
        //     currency = currency,
        //     merchantId = merchantId,
        //     description = description,
        //     singleUse = true,
        //     ttlSeconds = 300  // 5-minute expiry
        // )
        // val token = PravaSdk.createScopedToken(tokenRequest)

        val token = PaymentToken(
            tokenId = "prava_${System.currentTimeMillis()}_${amount}",
            amount = amount,
            currency = currency,
            merchantId = merchantId,
            expiresAt = System.currentTimeMillis() + 300_000, // 5 min
            isUsed = false
        )

        Log.i(TAG, "✅ Scoped token created: ${token.tokenId}")
        return token
    }

    /**
     * Confirm a payment using a previously created scoped token.
     *
     * @param token The scoped token from [requestScopedToken]
     * @return [PaymentResult] indicating success or failure
     */
    suspend fun confirmPayment(token: PaymentToken): PaymentResult {
        if (token.isExpired) {
            Log.e(TAG, "❌ Token expired: ${token.tokenId}")
            return PaymentResult(
                success = false,
                message = "Payment token expired. Please re-authorize.",
                transactionId = null
            )
        }

        if (token.isUsed) {
            Log.e(TAG, "❌ Token already used: ${token.tokenId}")
            return PaymentResult(
                success = false,
                message = "Payment token already used.",
                transactionId = null
            )
        }

        // TODO: Replace with actual Prava SDK confirmation
        // val result = PravaSdk.confirmPayment(token.tokenId)

        Log.i(TAG, "✅ Payment confirmed: ${token.tokenId}")
        return PaymentResult(
            success = true,
            message = "Payment of ${token.currency} ${token.amount / 100.0} confirmed.",
            transactionId = "txn_${System.currentTimeMillis()}"
        )
    }
}

/**
 * A scoped, single-use payment token.
 */
data class PaymentToken(
    val tokenId: String,
    val amount: Long,
    val currency: String,
    val merchantId: String,
    val expiresAt: Long,
    var isUsed: Boolean = false
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
}

/**
 * Result of a payment confirmation.
 */
data class PaymentResult(
    val success: Boolean,
    val message: String,
    val transactionId: String?
)
