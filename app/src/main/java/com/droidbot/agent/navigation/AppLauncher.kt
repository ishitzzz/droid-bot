package com.droidbot.agent.navigation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * AppLauncher — Opens apps via Android Intents.
 *
 * This works on ALL phones regardless of launcher/UI.
 * Instead of tapping through the home screen (which varies per OEM),
 * we use the system's Intent mechanism to launch apps directly.
 *
 * Common apps are mapped by keyword so the LLM can say "open chrome"
 * and we resolve it to the correct package/intent.
 */
object AppLauncher {

    private const val TAG = "AppLauncher"

    /** Map of common app keywords to package names. */
    private val APP_MAP = mapOf(
        // Browsers
        "chrome" to "com.android.chrome",
        "firefox" to "org.mozilla.firefox",
        "brave" to "com.brave.browser",
        "edge" to "com.microsoft.emmx",
        "samsung browser" to "com.sec.android.app.sbrowser",
        "opera" to "com.opera.browser",

        // Social
        "youtube" to "com.google.android.youtube",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "whatsapp" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "snapchat" to "com.snapchat.android",
        "reddit" to "com.reddit.frontpage",
        "tiktok" to "com.zhiliaoapp.musically",
        "threads" to "com.instagram.barcelona",
        "linkedin" to "com.linkedin.android",

        // Utilities
        "settings" to "com.android.settings",
        "camera" to "com.android.camera",
        "calculator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "calendar" to "com.google.android.calendar",
        "files" to "com.google.android.documentsui",
        "phone" to "com.android.dialer",
        "contacts" to "com.android.contacts",
        "messages" to "com.google.android.apps.messaging",

        // Maps & Travel
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "uber" to "com.ubercab",
        "ola" to "com.olacabs.customer",

        // Shopping
        "amazon" to "in.amazon.mShop.android.shopping",
        "flipkart" to "com.flipkart.android",
        "myntra" to "com.myntra.android",
        "swiggy" to "in.swiggy.android",
        "zomato" to "com.application.zomato",

        // Payments
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "google pay" to "com.google.android.apps.nbu.paisa.user",
        "paytm" to "net.one97.paytm",
        "phonepe" to "com.phonepe.app",

        // Google
        "gmail" to "com.google.android.gm",
        "google" to "com.google.android.googlequicksearchbox",
        "play store" to "com.android.vending",
        "photos" to "com.google.android.apps.photos",
        "drive" to "com.google.android.apps.docs",
        "sheets" to "com.google.android.apps.docs.editors.sheets",
        "docs" to "com.google.android.apps.docs.editors.docs",

        // Entertainment
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "prime video" to "com.amazon.avod.thirdpartyclient",
        "hotstar" to "in.startv.hotstar",
        "jio cinema" to "com.jio.media.ondemand"
    )

    /**
     * Attempt to launch an app by keyword.
     * Returns true if the app was successfully launched.
     */
    fun launchApp(context: Context, appKeyword: String): Boolean {
        val keyword = appKeyword.lowercase().trim()

        // Look up the package name from keywords
        val packageName = APP_MAP[keyword]
            ?: APP_MAP.entries.find { keyword.contains(it.key) }?.value
            ?: findPackageByName(context, keyword)

        if (packageName != null) {
            return launchByPackage(context, packageName)
        }

        Log.w(TAG, "App not found for keyword: $appKeyword")
        return false
    }

    /**
     * Launch an app by its package name using the launcher intent.
     */
    fun launchByPackage(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "Launched: $packageName")
                true
            } else {
                Log.w(TAG, "No launch intent for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            false
        }
    }

    /**
     * Open a URL in the default browser.
     */
    fun openUrl(context: Context, url: String): Boolean {
        return try {
            val fullUrl = if (!url.startsWith("http")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.i(TAG, "Opened URL: $fullUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
            false
        }
    }

    /**
     * Open Settings to a specific page.
     */
    fun openSettings(context: Context, page: String = ""): Boolean {
        return try {
            val action = when (page.lowercase()) {
                "wifi", "network" -> android.provider.Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
                "display" -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
                "sound" -> android.provider.Settings.ACTION_SOUND_SETTINGS
                "battery" -> android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
                "about" -> android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS
                "apps" -> android.provider.Settings.ACTION_APPLICATION_SETTINGS
                "location" -> android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
                else -> android.provider.Settings.ACTION_SETTINGS
            }
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.i(TAG, "Opened Settings: $page")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Settings: $page", e)
            false
        }
    }

    /**
     * Try to find a package by searching installed apps.
     */
    private fun findPackageByName(context: Context, name: String): String? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.firstOrNull { app ->
            val label = pm.getApplicationLabel(app).toString().lowercase()
            label.contains(name) || name.contains(label)
        }?.packageName
    }
}
