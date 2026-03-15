package com.droidbot.agent.navigation

import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

/**
 * UITreeParser — The Node-Numbering System.
 *
 * Converts the [AccessibilityNodeInfo] tree into a compact, numbered
 * text representation that the LLM can reason about.
 *
 * Output format:
 * ```
 * [Package: com.uber.android]
 * [1] FrameLayout
 *   [2] TextView "Welcome to Uber" (not-clickable)
 *   [3] EditText "Where to?" (editable, clickable)
 *   [4] Button "Search" (clickable, focusable)
 *     [5] ImageView (content-desc: "search icon")
 * ```
 *
 * The numbered IDs are used by [ActionExecutor] to target specific nodes.
 */
class UITreeParser {

    companion object {
        private const val TAG = "UITreeParser"
        private const val MAX_DEPTH = 15
        private const val MAX_NODES = 200
    }

    /** In-memory map of node number → AccessibilityNodeInfo for current snapshot. */
    private val nodeMap = mutableMapOf<Int, AccessibilityNodeInfo>()
    private var nodeCounter = 0

    /**
     * Parse the accessibility tree rooted at [rootNode] into a [UISnapshot].
     */
    fun parse(rootNode: AccessibilityNodeInfo, packageName: String): UISnapshot {
        nodeMap.clear()
        nodeCounter = 0

        val builder = StringBuilder()
        builder.appendLine("[Package: $packageName]")
        traverseNode(rootNode, builder, depth = 0)

        return UISnapshot(
            packageName = packageName,
            numberedTree = builder.toString(),
            nodeCount = nodeCounter,
            nodeMap = nodeMap.toMap()
        )
    }

    /**
     * Retrieve a previously parsed node by its number.
     */
    fun getNode(nodeNumber: Int): AccessibilityNodeInfo? = nodeMap[nodeNumber]

    /**
     * Recursively traverse the accessibility tree and build the numbered representation.
     */
    private fun traverseNode(
        node: AccessibilityNodeInfo,
        builder: StringBuilder,
        depth: Int
    ) {
        if (depth > MAX_DEPTH || nodeCounter >= MAX_NODES) return

        // Skip invisible or system-level nodes
        if (!node.isVisibleToUser) return

        nodeCounter++
        val nodeId = nodeCounter
        nodeMap[nodeId] = node

        val indent = "  ".repeat(depth)
        val className = node.className?.toString()?.substringAfterLast('.') ?: "View"
        val text = node.text?.toString()?.take(50) // Truncate long text
        val contentDesc = node.contentDescription?.toString()?.take(50)
        val viewId = node.viewIdResourceName?.substringAfterLast('/')

        // Build the node line
        builder.append("$indent[$nodeId] $className")

        // Add text/content description
        if (!text.isNullOrBlank()) {
            builder.append(" \"$text\"")
        }
        if (!contentDesc.isNullOrBlank()) {
            builder.append(" (content-desc: \"$contentDesc\")")
        }
        if (!viewId.isNullOrBlank()) {
            builder.append(" [id:$viewId]")
        }

        // Add interaction flags
        val flags = mutableListOf<String>()
        if (node.isClickable) flags.add("clickable")
        if (node.isLongClickable) flags.add("long-clickable")
        if (node.isEditable) flags.add("editable")
        if (node.isCheckable) flags.add(if (node.isChecked) "checked" else "unchecked")
        if (node.isScrollable) flags.add("scrollable")
        if (node.isFocusable) flags.add("focusable")

        if (flags.isNotEmpty()) {
            builder.append(" {${flags.joinToString(", ")}}")
        }

        builder.appendLine()

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, builder, depth + 1)
        }
    }
}

/**
 * Immutable snapshot of the UI state at a point in time.
 */
data class UISnapshot(
    val packageName: String,
    val numberedTree: String,
    val nodeCount: Int,
    val nodeMap: Map<Int, AccessibilityNodeInfo>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /** True if the tree is empty or has very few nodes (likely unreadable). */
    val isUnreadable: Boolean get() = nodeCount < 3

    override fun toString(): String = numberedTree
}
