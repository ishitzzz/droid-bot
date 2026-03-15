package com.droidbot.agent.navigation

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.droidbot.agent.service.DroidBotService

/**
 * ActionExecutor — The Hands of the Agent.
 *
 * Translates LLM-generated commands into real Android accessibility actions.
 *
 * Supported commands:
 * - `tap(id)` → Click the node with the given number
 * - `long_tap(id)` → Long-click the node
 * - `input(id, text)` → Focus the node and type text
 * - `scroll(direction)` → Scroll up/down/left/right
 * - `back()` → Press the system back button
 * - `home()` → Press the system home button
 * - `tap_xy(x, y)` → Tap at exact screen coordinates (self-healing fallback)
 */
class ActionExecutor(private val service: DroidBotService) {

    companion object {
        private const val TAG = "ActionExecutor"
    }

    /**
     * Parse and execute a command string from the LLM.
     *
     * @return [ActionResult] with success/failure and description.
     */
    fun execute(command: String): ActionResult {
        Log.d(TAG, "Executing: $command")

        return try {
            when {
                command.startsWith("tap(") -> {
                    val nodeId = extractSingleInt(command)
                    performTap(nodeId)
                }
                command.startsWith("long_tap(") -> {
                    val nodeId = extractSingleInt(command)
                    performLongTap(nodeId)
                }
                command.startsWith("input(") -> {
                    val (nodeId, text) = extractIdAndText(command)
                    performInput(nodeId, text)
                }
                command.startsWith("scroll(") -> {
                    val direction = extractString(command)
                    performScroll(direction)
                }
                command.startsWith("tap_xy(") -> {
                    val (x, y) = extractTwoFloats(command)
                    performTapXY(x, y)
                }
                command == "back()" -> performBack()
                command == "home()" -> performHome()
                command == "recents()" -> performRecents()
                else -> ActionResult(false, "Unknown command: $command")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action execution failed: $command", e)
            ActionResult(false, "Exception: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════
    // Node-Based Actions
    // ═══════════════════════════════════════════════════

    private fun performTap(nodeId: Int): ActionResult {
        val node = service.uiTreeParser.getNode(nodeId)
            ?: return ActionResult(false, "Node [$nodeId] not found in tree")

        // Walk up to find a clickable ancestor if the node itself isn't clickable
        val clickable = findClickableNode(node)
        return if (clickable != null) {
            val result = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ActionResult(result, if (result) "Tapped node [$nodeId]" else "Tap failed on [$nodeId]")
        } else {
            // Fallback: get bounds and tap by coordinates
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            service.dispatchTap(rect.centerX().toFloat(), rect.centerY().toFloat())
            ActionResult(true, "Tapped [$nodeId] via coordinates (${rect.centerX()}, ${rect.centerY()})")
        }
    }

    private fun performLongTap(nodeId: Int): ActionResult {
        val node = service.uiTreeParser.getNode(nodeId)
            ?: return ActionResult(false, "Node [$nodeId] not found")

        val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        return ActionResult(result, if (result) "Long-tapped [$nodeId]" else "Long-tap failed on [$nodeId]")
    }

    private fun performInput(nodeId: Int, text: String): ActionResult {
        val node = service.uiTreeParser.getNode(nodeId)
            ?: return ActionResult(false, "Node [$nodeId] not found")

        // Focus the node first
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // Clear existing text
        val clearArgs = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, ""
            )
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearArgs)

        // Set new text
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
            )
        }
        val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return ActionResult(result, if (result) "Input \"$text\" into [$nodeId]" else "Input failed on [$nodeId]")
    }

    // ═══════════════════════════════════════════════════
    // Scroll Actions
    // ═══════════════════════════════════════════════════

    private fun performScroll(direction: String): ActionResult {
        val displayMetrics = service.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()

        when (direction.lowercase()) {
            "up" -> service.dispatchSwipe(width / 2, height * 0.7f, width / 2, height * 0.3f)
            "down" -> service.dispatchSwipe(width / 2, height * 0.3f, width / 2, height * 0.7f)
            "left" -> service.dispatchSwipe(width * 0.8f, height / 2, width * 0.2f, height / 2)
            "right" -> service.dispatchSwipe(width * 0.2f, height / 2, width * 0.8f, height / 2)
            else -> return ActionResult(false, "Unknown scroll direction: $direction")
        }
        return ActionResult(true, "Scrolled $direction")
    }

    // ═══════════════════════════════════════════════════
    // Coordinate-Based Actions (Self-Healing)
    // ═══════════════════════════════════════════════════

    private fun performTapXY(x: Float, y: Float): ActionResult {
        service.dispatchTap(x, y)
        return ActionResult(true, "Tapped at coordinates ($x, $y)")
    }

    // ═══════════════════════════════════════════════════
    // System Navigation
    // ═══════════════════════════════════════════════════

    private fun performBack(): ActionResult {
        val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        return ActionResult(result, if (result) "Pressed BACK" else "BACK failed")
    }

    private fun performHome(): ActionResult {
        val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        return ActionResult(result, if (result) "Pressed HOME" else "HOME failed")
    }

    private fun performRecents(): ActionResult {
        val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        return ActionResult(result, if (result) "Opened RECENTS" else "RECENTS failed")
    }

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    /** Walk up the tree to find the nearest clickable ancestor. */
    private fun findClickableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    private fun extractSingleInt(cmd: String): Int {
        return cmd.substringAfter("(").substringBefore(")").trim().toInt()
    }

    private fun extractString(cmd: String): String {
        return cmd.substringAfter("(").substringBefore(")").trim().removeSurrounding("\"")
    }

    private fun extractIdAndText(cmd: String): Pair<Int, String> {
        val inner = cmd.substringAfter("(").substringBefore(")")
        val parts = inner.split(",", limit = 2)
        return Pair(parts[0].trim().toInt(), parts[1].trim().removeSurrounding("\""))
    }

    private fun extractTwoFloats(cmd: String): Pair<Float, Float> {
        val inner = cmd.substringAfter("(").substringBefore(")")
        val parts = inner.split(",")
        return Pair(parts[0].trim().toFloat(), parts[1].trim().toFloat())
    }
}

/**
 * Result of an agent action.
 */
data class ActionResult(
    val success: Boolean,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
