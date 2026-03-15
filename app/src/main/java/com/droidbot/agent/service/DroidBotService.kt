package com.droidbot.agent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.droidbot.agent.navigation.ActionExecutor
import com.droidbot.agent.navigation.UITreeParser
import com.droidbot.agent.brain.NavigationBrain
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DroidBotService — The Eyes & Hands of the Agent.
 *
 * An AccessibilityService that:
 * 1. Captures the live UI tree via [AccessibilityNodeInfo]
 * 2. Converts it to numbered text via [UITreeParser]
 * 3. Feeds it to [NavigationBrain] for ReAct decisions
 * 4. Executes actions via [ActionExecutor]
 *
 * This is the core runtime loop of DroidBot.
 */
class DroidBotService : AccessibilityService() {

    companion object {
        private const val TAG = "DroidBotService"
        private const val OWN_PACKAGE = "com.droidbot.agent"
        private const val DEBOUNCE_MS = 300L

        /** Singleton reference so other components can access the service. */
        @Volatile
        var instance: DroidBotService? = null
            private set

        /** Observable service connection state. */
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var debounceJob: Job? = null

    /** The parser that converts AccessibilityNodeInfo → numbered text. */
    val uiTreeParser = UITreeParser()

    /** The executor that dispatches tap/scroll/input commands. */
    lateinit var actionExecutor: ActionExecutor
        private set

    /** The brain that decides what to do next. */
    private var navigationBrain: NavigationBrain? = null

    // ═══════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        actionExecutor = ActionExecutor(this)
        Log.i(TAG, "🤖 DroidBot Service connected — ready to navigate.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isConnected.value = false
        serviceScope.cancel()
        Log.i(TAG, "🤖 DroidBot Service destroyed.")
    }

    override fun onInterrupt() {
        Log.w(TAG, "DroidBot Service interrupted.")
    }

    // ═══════════════════════════════════════════════════
    // Event Handling
    // ═══════════════════════════════════════════════════

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // CRITICAL: Ignore events from DroidBot itself to prevent feedback loop
        val eventPackage = event.packageName?.toString() ?: return
        if (eventPackage == OWN_PACKAGE || eventPackage == "$OWN_PACKAGE.debug") return

        // Ignore system UI events that aren't useful for navigation
        if (eventPackage == "com.android.systemui") return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Debounce rapid events — only process after UI settles
                debounceJob?.cancel()
                debounceJob = serviceScope.launch {
                    delay(DEBOUNCE_MS)
                    processUIChange(eventPackage)
                }
            }
        }
    }

    /**
     * Captures the current UI state and feeds it to the NavigationBrain.
     */
    private suspend fun processUIChange(currentPackage: String) {
        val rootNode = rootInActiveWindow ?: return
        try {
            // Double-check we're not reading our own UI
            val rootPackage = rootNode.packageName?.toString() ?: "unknown"
            if (rootPackage == OWN_PACKAGE || rootPackage == "$OWN_PACKAGE.debug") {
                return
            }

            val uiSnapshot = uiTreeParser.parse(rootNode, rootPackage)

            // Feed to NavigationBrain if a task is active
            navigationBrain?.onUIStateChanged(uiSnapshot)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing UI change", e)
        } 
        // Note: rootNode is intentionally NOT manually recycled here because
        // it is stored in the UISnapshot nodeMap for ActionExecutor to use.
        // The OS will garbage collect it or pool it safely.
    }

    /**
     * Called by the Brain to forcefully capture the current UI state 
     * if the event-driven system is too slow or misses an update.
     */
    suspend fun processUIChangeFromBrain() {
        processUIChange("manual_pull")
    }

    // ═══════════════════════════════════════════════════
    // Public API — Called by NavigationBrain / UI
    // ═══════════════════════════════════════════════════

    /**
     * Attach a NavigationBrain to start processing tasks.
     */
    fun attachBrain(brain: NavigationBrain) {
        this.navigationBrain = brain
        Log.i(TAG, "Brain attached — agent is now agentic.")
    }

    /**
     * Detach the brain — service stays alive but stops processing.
     */
    fun detachBrain() {
        this.navigationBrain = null
        Log.i(TAG, "Brain detached — agent is now passive.")
    }

    /**
     * Get a fresh snapshot of the current UI tree.
     * Used for on-demand reads (not event-driven).
     */
    fun captureCurrentUI(): String? {
        val rootNode = rootInActiveWindow ?: return null
        return try {
            val snapshot = uiTreeParser.parse(rootNode, rootNode.packageName?.toString() ?: "unknown")
            snapshot.numberedTree
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Dispatch a coordinate-based tap gesture (used by SelfHealingEngine).
     */
    fun dispatchTap(x: Float, y: Float, durationMs: Long = 100L) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Dispatched tap at ($x, $y)")
    }

    /**
     * Dispatch a swipe gesture for scrolling.
     */
    fun dispatchSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        durationMs: Long = 300L
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Dispatched swipe from ($startX,$startY) to ($endX,$endY)")
    }
}
