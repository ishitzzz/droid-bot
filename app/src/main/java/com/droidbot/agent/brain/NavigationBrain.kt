package com.droidbot.agent.brain

import android.util.Log
import com.droidbot.agent.hive.SharedKnowledgeBase
import com.droidbot.agent.hive.UIMap
import com.droidbot.agent.hive.NavigationStep
import com.droidbot.agent.identity.IdentityVault
import com.droidbot.agent.navigation.ActionExecutor
import com.droidbot.agent.navigation.ActionResult
import com.droidbot.agent.navigation.SelfHealingEngine
import com.droidbot.agent.navigation.UISnapshot
import com.droidbot.agent.service.DroidBotService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NavigationBrain — The ReAct Loop Controller.
 *
 * Implements the Reasoning + Action (ReAct) pattern:
 *
 * ```
 * ┌─────────────────────────────────────────────┐
 * │                 OBSERVE                      │
 * │   UITreeParser → UI Snapshot (numbered text) │
 * └─────────────┬───────────────────────────────┘
 *               ▼
 * ┌─────────────────────────────────────────────┐
 * │                  THINK                       │
 * │   HybridRouter → Gemini Nano / Pro          │
 * │   "Given this UI and task, what's next?"     │
 * └─────────────┬───────────────────────────────┘
 *               ▼
 * ┌─────────────────────────────────────────────┐
 * │                   ACT                        │
 * │   ActionExecutor → tap(3), input(5, "SFO")  │
 * └─────────────┬───────────────────────────────┘
 *               ▼
 * ┌─────────────────────────────────────────────┐
 * │                 REFLECT                      │
 * │   Did the UI change? Goal achieved?          │
 * │   If failed → pivot / self-heal / retry      │
 * └─────────────────────────────────────────────┘
 * ```
 *
 * Self-Correction Pivots:
 * - App missing → Play Store install → retry
 * - Login required → IdentityVault → populate fields
 * - Path failed → competitor app or web fallback
 */
@Singleton
class NavigationBrain @Inject constructor(
    private val hybridRouter: HybridRouter,
    private val selfHealingEngine: SelfHealingEngine,
    private val knowledgeBase: SharedKnowledgeBase,
    private val identityVault: IdentityVault
) {

    companion object {
        private const val TAG = "NavigationBrain"
        private const val MAX_STEPS = 50
        private const val MAX_RETRIES = 3
        private const val STEP_DELAY_MS = 500L
    }

    /** Current agent state. */
    private val _state = MutableStateFlow(AgentState.IDLE)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    /** Action log for the current task. */
    private val _actionLog = MutableStateFlow<List<ActionLogEntry>>(emptyList())
    val actionLog: StateFlow<List<ActionLogEntry>> = _actionLog.asStateFlow()

    /** The current task being executed. */
    private var currentTask: AgentTask? = null
    private var stepCount = 0
    private var retryCount = 0

    /** Recorded navigation steps for Hive upload. */
    private val recordedSteps = mutableListOf<NavigationStep>()

    /** Latest UI snapshot (updated by DroidBotService). */
    @Volatile
    private var latestSnapshot: UISnapshot? = null

    private val brainScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ═══════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════

    /**
     * Start executing a task autonomously.
     */
    fun startTask(task: AgentTask) {
        if (_state.value == AgentState.RUNNING) {
            Log.w(TAG, "Already running a task. Stop current task first.")
            return
        }

        currentTask = task
        stepCount = 0
        retryCount = 0
        recordedSteps.clear()
        _actionLog.value = emptyList()
        _state.value = AgentState.RUNNING

        log("🚀 Task started: ${task.description}")

        brainScope.launch {
            try {
                executeReActLoop(task)
            } catch (e: Exception) {
                log("💥 Fatal error: ${e.message}")
                Log.e(TAG, "ReAct loop crashed", e)
                _state.value = AgentState.ERROR
            }
        }
    }

    /**
     * Stop the current task.
     */
    fun stopTask() {
        _state.value = AgentState.IDLE
        currentTask = null
        log("⏹️ Task stopped by user.")
    }

    /**
     * Called by DroidBotService when the UI changes.
     */
    fun onUIStateChanged(snapshot: UISnapshot) {
        latestSnapshot = snapshot
    }

    // ═══════════════════════════════════════════════════
    // ReAct Loop
    // ═══════════════════════════════════════════════════

    private suspend fun executeReActLoop(task: AgentTask) {
        // Check Hive for known navigation paths first
        val knownPath = knowledgeBase.queryRelevantPaths(task.description, task.targetPackage)
        if (knownPath != null) {
            log("🐝 Hive: Found known path with ${knownPath.steps.size} steps")
        }

        while (_state.value == AgentState.RUNNING && stepCount < MAX_STEPS) {
            stepCount++

            // ── OBSERVE ──
            val snapshot = waitForUISnapshot()
            if (snapshot == null) {
                log("⚠️ No UI snapshot available. Waiting...")
                delay(1000)
                continue
            }

            // Check if UI is unreadable → trigger self-healing
            if (snapshot.isUnreadable) {
                log("🔬 UI tree unreadable — triggering self-healing vision")
                val visionDescription = selfHealingEngine.describeScreen()
                if (visionDescription != null) {
                    log("👁️ Vision sees: $visionDescription")
                }
            }

            // ── THINK ──
            val prompt = buildPrompt(task, snapshot, knownPath)
            val response = try {
                hybridRouter.infer(prompt, task.complexity)
            } catch (e: Exception) {
                // If the Gemini API fails (e.g. 404 due to wrong model name, or no internet)
                val errorMsg = "❌ API Error: ${e.localizedMessage ?: e.message}"
                log(errorMsg)
                Log.e(TAG, "Inference Exception", e)
                
                if (retryCount++ < MAX_RETRIES) {
                    log("⚠️ Retrying in 3 seconds ($retryCount/3)...")
                    delay(3000)
                    continue
                } else {
                    _state.value = AgentState.ERROR
                    log("💀 Max retries exceeded. The selected Gemini model may be invalid or you are offline.")
                    return
                }
            }

            // ── PARSE RESPONSE ──
            val agentResponse = parseAgentResponse(response)

            // Handle special states
            when (agentResponse.status) {
                ResponseStatus.GOAL_ACHIEVED -> {
                    _state.value = AgentState.COMPLETED
                    log("✅ Goal achieved: ${agentResponse.reasoning}")
                    uploadToHive(task)
                    return
                }
                ResponseStatus.NEED_LOGIN -> {
                    log("🔐 Login required — checking IdentityVault")
                    handleLogin(snapshot)
                }
                ResponseStatus.APP_NOT_FOUND -> {
                    log("📦 App not installed — navigating to Play Store")
                    handleAppInstall(task.targetPackage)
                }
                ResponseStatus.PIVOT_NEEDED -> {
                    log("🔄 Pivoting: ${agentResponse.reasoning}")
                    // The LLM will have chosen a new path in its action
                }
                ResponseStatus.CONTINUE -> {
                    // Normal flow — execute the action
                }
            }

            // ── ACT ──
            if (agentResponse.action.isNotBlank()) {
                log("🎯 Step $stepCount: ${agentResponse.action} | ${agentResponse.reasoning}")

                val service = DroidBotService.instance
                if (service != null) {
                    val result = service.actionExecutor.execute(agentResponse.action)

                    // Record step for Hive
                    recordedSteps.add(
                        NavigationStep(
                            action = agentResponse.action,
                            reasoning = agentResponse.reasoning,
                            uiContext = snapshot.numberedTree.take(500),
                            success = result.success,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    if (!result.success) {
                        log("⚠️ Action failed: ${result.description}")
                        // Try self-healing if action failed
                        if (snapshot.isUnreadable) {
                            val healResult = selfHealingEngine.visualTap(
                                targetDescription = agentResponse.reasoning,
                                taskContext = task.description
                            )
                            log("🔬 Self-heal result: ${healResult.description}")
                        }
                    }
                }
            }

            // ── REFLECT ──
            // Wait for UI to settle after action
            delay(STEP_DELAY_MS)
            retryCount = 0 // Reset retry count on successful step
        }

        if (stepCount >= MAX_STEPS) {
            _state.value = AgentState.ERROR
            log("⚠️ Max steps ($MAX_STEPS) reached. Task may be incomplete.")
        }
    }

    // ═══════════════════════════════════════════════════
    // Prompt Building
    // ═══════════════════════════════════════════════════

    private fun buildPrompt(
        task: AgentTask,
        snapshot: UISnapshot,
        knownPath: UIMap?
    ): String {
        return buildString {
            appendLine("You are DroidBot, an autonomous Android agent.")
            appendLine("Your goal is to dynamically navigate the user's phone to complete their task.")
            appendLine("Do NOT give up if an app isn't found immediately — search for it, use the app drawer, or find an alternative way to complete the task.")
            appendLine()
            appendLine("## TASK")
            appendLine(task.description)
            appendLine()
            appendLine("## CURRENT SCREEN (${snapshot.packageName})")
            if (snapshot.numberedTree.isBlank()) {
                appendLine("(Screen is blank or unreadable)")
            } else {
                appendLine(snapshot.numberedTree)
            }
            appendLine()
            appendLine("## AVAILABLE COMMANDS")
            appendLine("- tap(id) — Tap the element with the given number")
            appendLine("- long_tap(id) — Long-press the element")
            appendLine("- input(id, \"text\") — Type text into an editable field")
            appendLine("- scroll(up|down|left|right) — Scroll the screen")
            appendLine("- back() — Press the system back button")
            appendLine("- home() — Go to home screen")
            appendLine()
            if (knownPath != null) {
                appendLine("## KNOWN PATH FROM HIVE (use as reference)")
                knownPath.steps.forEachIndexed { i, step ->
                    appendLine("  ${i + 1}. ${step.action} — ${step.reasoning}")
                }
                appendLine()
            }
            appendLine("## PREVIOUS ACTIONS (this session)")
            val recentActions = _actionLog.value.takeLast(6)
            if (recentActions.isEmpty()) {
                appendLine("(None yet)")
            } else {
                recentActions.forEach { appendLine("  - ${it.message}") }
            }
            appendLine()
            appendLine("## RESPONSE FORMAT")
            appendLine("You MUST respond ONLY with a valid JSON object. Do not wrap it in markdown. Do not add conversational text.")
            appendLine("{")
            appendLine("  \"status\": \"CONTINUE\", // Or GOAL_ACHIEVED, NEED_LOGIN, APP_NOT_FOUND, PIVOT_NEEDED")
            appendLine("  \"action\": \"tap(4)\", // Only ONE command from the list above")
            appendLine("  \"reasoning\": \"Tapping the search bar to type the weather query\"")
            appendLine("}")
        }
    }

    // ═══════════════════════════════════════════════════
    private fun parseAgentResponse(rawResponse: String): AgentResponse {
        // Clean up markdown code blocks if the LLM wrapped the JSON
        var cleanResponse = rawResponse.trim()
        if (cleanResponse.startsWith("```")) {
            cleanResponse = cleanResponse.lines().drop(1).dropLast(1).joinToString("\n")
        }

        // Try to find anything that looks like a JSON object
        val jsonStart = cleanResponse.indexOf('{')
        val jsonEnd = cleanResponse.lastIndexOf('}')
        
        val json = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleanResponse.substring(jsonStart, jsonEnd + 1)
        } else cleanResponse

        return try {
            val status = extractJsonField(json, "status", "CONTINUE")
            val action = extractJsonField(json, "action", "")
            val reasoning = extractJsonField(json, "reasoning", "Extracted from raw text")

            AgentResponse(
                status = ResponseStatus.fromString(status),
                action = action,
                reasoning = reasoning
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JSON, using raw response", e)
            AgentResponse(ResponseStatus.CONTINUE, "", "Raw response: $rawResponse")
        }
    }

    private fun extractJsonField(json: String, field: String, default: String): String {
        // Handle both single-line and multi-line formatted JSON
        val regex = """"$field"\s*:\s*"([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(json)?.groupValues?.get(1) ?: default
    }

    // ═══════════════════════════════════════════════════
    // Self-Correction Handlers
    // ═══════════════════════════════════════════════════

    private suspend fun handleLogin(snapshot: UISnapshot) {
        val creds = identityVault.getCredentials(snapshot.packageName)
        if (creds != null) {
            log("🔑 Found credentials for ${snapshot.packageName}")
            // The next ReAct iteration will detect login fields and populate them
        } else {
            log("⚠️ No credentials found for ${snapshot.packageName}. User intervention needed.")
            _state.value = AgentState.WAITING_FOR_USER
        }
    }

    private suspend fun handleAppInstall(targetPackage: String) {
        val service = DroidBotService.instance ?: return

        // Navigate to Play Store search
        val playStoreCommand = "tap_xy(540, 960)" // Will be replaced by actual Play Store navigation
        service.actionExecutor.execute("home()")
        delay(500)

        log("📦 Opening Play Store to install: $targetPackage")
        // TODO: Implement full Play Store navigation flow
        // 1. home() → find Play Store → tap → search → type package → install → wait → open
    }

    private suspend fun uploadToHive(task: AgentTask) {
        if (recordedSteps.isNotEmpty()) {
            val uiMap = UIMap(
                appPackage = task.targetPackage,
                appVersion = "", // TODO: detect via PackageManager
                taskDescription = task.description,
                steps = recordedSteps.toList(),
                successRate = recordedSteps.count { it.success }.toFloat() / recordedSteps.size,
                timestamp = System.currentTimeMillis()
            )
            knowledgeBase.uploadUIMap(uiMap)
            log("🐝 Uploaded ${recordedSteps.size} steps to the Hive")
        }
    }

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    private suspend fun waitForUISnapshot(): UISnapshot? {
        val service = DroidBotService.instance
        if (service != null) {
            // Proactively request a fresh snapshot from the service
            val freshTree = service.captureCurrentUI()
            if (freshTree != null) {
                // The service.captureCurrentUI() returns a String, but we need the full UISnapshot.
                // It's better to tell the service to just process the UI change so we get the full object.
                service.processUIChangeFromBrain()
            }
        }

        // Wait up to 10 seconds for a fresh snapshot
        repeat(20) {
            latestSnapshot?.let { return it }
            delay(500)
        }
        return null
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        _actionLog.value = _actionLog.value + ActionLogEntry(
            message = message,
            timestamp = System.currentTimeMillis()
        )
    }
}

// ═══════════════════════════════════════════════════════
// Data Classes
// ═══════════════════════════════════════════════════════

enum class AgentState {
    IDLE, RUNNING, COMPLETED, ERROR, WAITING_FOR_USER
}

data class AgentTask(
    val description: String,
    val targetPackage: String = "",
    val complexity: TaskComplexity = TaskComplexity.MEDIUM,
    val fallbackPackages: List<String> = emptyList()
)

enum class TaskComplexity {
    /** Simple — single tap, can be handled by Nano. */
    LOW,
    /** Medium — multi-step flow, may need Pro for planning. */
    MEDIUM,
    /** High — complex reasoning, booking, payments. Always Pro. */
    HIGH
}

data class ActionLogEntry(
    val message: String,
    val timestamp: Long
)

data class AgentResponse(
    val status: ResponseStatus,
    val action: String,
    val reasoning: String
)

enum class ResponseStatus {
    CONTINUE, GOAL_ACHIEVED, NEED_LOGIN, APP_NOT_FOUND, PIVOT_NEEDED;

    companion object {
        fun fromString(value: String): ResponseStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CONTINUE
        }
    }
}
