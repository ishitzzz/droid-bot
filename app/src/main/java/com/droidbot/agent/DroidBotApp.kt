package com.droidbot.agent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * DroidBot Application — The Digital Twin.
 *
 * Entry point for Hilt dependency injection. Initializes the
 * application-wide singletons: inference engines, knowledge base,
 * and identity vault.
 */
@HiltAndroidApp
class DroidBotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Global initialization happens via Hilt modules.
        // No manual singleton wiring needed.
    }
}
