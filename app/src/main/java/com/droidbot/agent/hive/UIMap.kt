package com.droidbot.agent.hive

/**
 * UIMap — A recorded navigation path.
 *
 * When DroidBot successfully completes a task, the sequence of steps
 * is recorded as a UIMap and uploaded to the Shared Knowledge Base.
 * Other DroidBot instances can query these maps to skip the "exploration"
 * phase and directly replay known paths.
 *
 * This is the DNA of The Hive — collective intelligence.
 */
data class UIMap(
    /** Package name of the app (e.g., "com.uber.android"). */
    val appPackage: String,

    /** App version string (e.g., "4.513.10000"). */
    val appVersion: String,

    /** Natural language description of the task (e.g., "Book a ride to SFO"). */
    val taskDescription: String,

    /** Ordered list of navigation steps. */
    val steps: List<NavigationStep>,

    /** Success rate (0.0 - 1.0) of the steps when executed. */
    val successRate: Float = 1.0f,

    /** When this map was recorded. */
    val timestamp: Long = System.currentTimeMillis(),

    /** Embedding vector for semantic search (populated by Vector DB). */
    val embedding: FloatArray? = null
) {
    /** Unique identifier for this map. */
    val id: String get() = "$appPackage:${taskDescription.hashCode()}:$timestamp"

    /** Whether this map is considered reliable (high success rate). */
    val isReliable: Boolean get() = successRate >= 0.8f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UIMap) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * A single step in a navigation path.
 */
data class NavigationStep(
    /** The command executed (e.g., "tap(4)", "input(3, \"SFO\")"). */
    val action: String,

    /** Why this action was chosen. */
    val reasoning: String,

    /** Abbreviated UI context at the time of this step. */
    val uiContext: String,

    /** Whether this step succeeded. */
    val success: Boolean,

    /** Timestamp of this step. */
    val timestamp: Long
)
