package com.droidbot.agent.identity

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IdentityVault — Secure Credential Store.
 *
 * Stores per-app credentials using [EncryptedSharedPreferences] backed
 * by Android Keystore. Every credential access requires biometric
 * authentication via [BiometricGate].
 *
 * Security model:
 * - Credentials encrypted at rest (AES-256-GCM via MasterKey)
 * - Key material never leaves the hardware security module
 * - Biometric required to decrypt any credential
 * - No plaintext credentials in memory longer than needed
 *
 * ┌────────────────────────────────────────────┐
 * │  IdentityVault                             │
 * │  ┌──────────────────────────────────────┐  │
 * │  │  EncryptedSharedPreferences          │  │
 * │  │  ┌──────────────────────────────┐    │  │
 * │  │  │  Android Keystore (HSM)     │    │  │
 * │  │  │  AES-256-GCM MasterKey      │    │  │
 * │  │  └──────────────────────────────┘    │  │
 * │  └──────────────────────────────────────┘  │
 * │                                            │
 * │  BiometricGate → unlock → decrypt → use    │
 * └────────────────────────────────────────────┘
 */
@Singleton
class IdentityVault @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "IdentityVault"
        private const val PREFS_NAME = "droidbot_vault"
        private const val KEY_PREFIX = "cred_"
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(true)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ═══════════════════════════════════════════════════
    // Credential Management
    // ═══════════════════════════════════════════════════

    /**
     * Store credentials for an app.
     *
     * @param packageName The app's package name (e.g., "com.uber.android")
     * @param credentials Map of credential fields (e.g., {"email": "...", "password": "..."})
     */
    fun storeCredentials(packageName: String, credentials: Map<String, String>) {
        val json = JSONObject(credentials).toString()
        encryptedPrefs.edit()
            .putString("$KEY_PREFIX$packageName", json)
            .apply()
        Log.i(TAG, "🔐 Credentials stored for: $packageName")
    }

    /**
     * Retrieve credentials for an app.
     * Returns null if no credentials are stored.
     *
     * @param packageName The app's package name
     * @return Map of credential fields, or null
     */
    fun getCredentials(packageName: String): Map<String, String>? {
        val json = encryptedPrefs.getString("$KEY_PREFIX$packageName", null) ?: return null

        return try {
            val jsonObj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            jsonObj.keys().forEach { key -> map[key] = jsonObj.getString(key) }
            Log.d(TAG, "🔑 Credentials retrieved for: $packageName")
            map
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse credentials for $packageName", e)
            null
        }
    }

    /**
     * Check if credentials exist for an app.
     */
    fun hasCredentials(packageName: String): Boolean {
        return encryptedPrefs.contains("$KEY_PREFIX$packageName")
    }

    /**
     * Delete credentials for an app.
     */
    fun deleteCredentials(packageName: String) {
        encryptedPrefs.edit()
            .remove("$KEY_PREFIX$packageName")
            .apply()
        Log.i(TAG, "🗑️ Credentials deleted for: $packageName")
    }

    /**
     * List all apps with stored credentials.
     */
    fun listStoredApps(): List<String> {
        return encryptedPrefs.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .map { it.removePrefix(KEY_PREFIX) }
    }

    /**
     * Wipe the entire vault. Use with caution.
     */
    fun wipeVault() {
        encryptedPrefs.edit().clear().apply()
        Log.w(TAG, "⚠️ Identity vault wiped")
    }
}
