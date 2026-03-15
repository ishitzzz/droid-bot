package com.droidbot.agent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.droidbot.agent.brain.*
import com.droidbot.agent.hive.SharedKnowledgeBase
import com.droidbot.agent.identity.IdentityVault
import com.droidbot.agent.navigation.AppLauncher
import com.droidbot.agent.navigation.SelfHealingEngine
import com.droidbot.agent.service.DroidBotService
import com.droidbot.agent.service.VoiceCommandService
import com.droidbot.agent.ui.screens.DashboardScreen
import com.droidbot.agent.ui.theme.DroidBotTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * MainActivity — DroidBot Entry Point.
 *
 * Single Activity architecture with Jetpack Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var activeBrain: NavigationBrain? = null
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Shared state flows that persist across recompositions
    private val brainState = MutableStateFlow(AgentState.IDLE)
    private val brainLog = MutableStateFlow<List<ActionLogEntry>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            DroidBotTheme {
                val isServiceConnected by DroidBotService.isConnected.collectAsState()
                val agentState by brainState.collectAsState()
                val actionLog by brainLog.collectAsState()
                
                var isListening by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        toggleVoiceCommandService(true)
                        isListening = true
                    } else {
                        Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                        isListening = false
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        isServiceConnected = isServiceConnected,
                        onEnableAccessibility = { openAccessibilitySettings() },
                        onStartTask = { taskDescription -> startAgentTask(taskDescription) },
                        onStopTask = { stopAgentTask() },
                        onClearLog = { clearAgentLog() },
                        isListening = isListening,
                        onToggleListening = { enable ->
                            if (enable) {
                                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    toggleVoiceCommandService(true)
                                    isListening = true
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                toggleVoiceCommandService(false)
                                isListening = false
                            }
                        },
                        agentState = agentState,
                        actionLog = actionLog
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val voiceCommand = intent?.getStringExtra("VOICE_COMMAND_TASK")
        if (!voiceCommand.isNullOrBlank()) {
            Log.i("MainActivity", "Received voice command intent: $voiceCommand")
            // Give UI a moment to compose if starting cold, then launch
            activityScope.launch {
                delay(500)
                startAgentTask(voiceCommand)
            }
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun startAgentTask(taskDescription: String) {
        val service = DroidBotService.instance
        if (service == null) {
            Toast.makeText(this, "Enable Accessibility Service first!", Toast.LENGTH_LONG).show()
            return
        }

        // Check API key
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Toast.makeText(this, "Missing GEMINI_API_KEY in local.properties!", Toast.LENGTH_LONG).show()
            return
        }

        // Create the brain
        val cloudInference = CloudInference()
        val brain = NavigationBrain(
            hybridRouter = HybridRouter(
                context = applicationContext,
                edgeInference = EdgeInference(applicationContext),
                cloudInference = cloudInference
            ),
            selfHealingEngine = SelfHealingEngine(cloudInference = cloudInference),
            knowledgeBase = SharedKnowledgeBase(),
            identityVault = IdentityVault(applicationContext)
        )

        activeBrain = brain
        service.attachBrain(brain)

        // Wire brain's state to our shared flows
        activityScope.launch {
            launch { brain.state.collect { brainState.value = it } }
            launch { brain.actionLog.collect { brainLog.value = it } }
        }

        Toast.makeText(this, "🤖 DroidBot starting: $taskDescription", Toast.LENGTH_SHORT).show()

        // Launch the task in background
        activityScope.launch {
            // Try to extract and launch an app from the task description
            val appLaunched = tryLaunchAppFromTask(taskDescription)

            if (appLaunched) {
                // Wait for the launched app to become ready on screen
                delay(3000)
            } else {
                // Do not force HOME! The user might want the agent to operate on the CURRENT screen.
                Log.i("MainActivity", "No app explicitly launched. Starting agent on the current screen.")
                delay(1000) // Brief delay before snapshot
            }

            // Start the ReAct loop now that we're on a real screen
            Log.i("MainActivity", "Starting Brain ReAct Loop for task: $taskDescription")
            brain.startTask(AgentTask(description = taskDescription))
        }
    }

    /**
     * Extract app name from the task and launch it via Intent.
     * Returns true if an app was successfully launched.
     */
    private fun tryLaunchAppFromTask(task: String): Boolean {
        val lower = task.lowercase()

        // Check for URL patterns
        val urlRegex = """(https?://\S+|www\.\S+|\w+\.(com|org|net|io)\S*)""".toRegex()
        val urlMatch = urlRegex.find(lower)
        if (urlMatch != null) {
            return AppLauncher.openUrl(this, urlMatch.value)
        }

        // Handle direct Google Search
        if (lower.startsWith("search ") || lower.startsWith("google ")) {
            val query = lower.removePrefix("search ").removePrefix("google ").trim()
            if (query.isNotEmpty()) {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                return AppLauncher.openUrl(this, "https://www.google.com/search?q=$encodedQuery")
            }
        }

        // Handle settings commands
        if (lower.contains("settings")) {
            return when {
                lower.contains("wifi") || lower.contains("wi-fi") -> AppLauncher.openSettings(this, Settings.ACTION_WIFI_SETTINGS)
                lower.contains("bluetooth") -> AppLauncher.openSettings(this, Settings.ACTION_BLUETOOTH_SETTINGS)
                lower.contains("display") -> AppLauncher.openSettings(this, Settings.ACTION_DISPLAY_SETTINGS)
                lower.contains("accessibility") -> AppLauncher.openSettings(this, Settings.ACTION_ACCESSIBILITY_SETTINGS)
                else -> AppLauncher.openSettings(this, Settings.ACTION_SETTINGS)
            }
        }

        // Fallback to searching installed apps by name
        val words = lower.split(" ")
        for (word in words) {
            if (word.length > 2 && word !in listOf("open", "launch", "start", "the", "app", "application")) {
                if (AppLauncher.launchApp(this, word)) {
                    return true
                }
            }
        }

        return false
    }

    private fun stopAgentTask() {
        activeBrain?.stopTask()
        DroidBotService.instance?.detachBrain()
        activeBrain = null
        brainState.value = AgentState.IDLE
        // Do NOT clear the log here so the user can see past tasks
        Toast.makeText(this, "⏹️ Agent stopped", Toast.LENGTH_SHORT).show()
    }

    fun clearAgentLog() {
        brainLog.value = emptyList()
        Toast.makeText(this, "🧹 Log cleared", Toast.LENGTH_SHORT).show()
    }

    private fun toggleVoiceCommandService(enable: Boolean) {
        val intent = Intent(this, VoiceCommandService::class.java)
        if (enable) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            stopService(intent)
        }
    }
}
