package com.droidbot.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.droidbot.agent.MainActivity
import com.droidbot.agent.R

/**
 * VoiceCommandService — The Ears of the Agent.
 *
 * Uses Android's built-in SpeechRecognizer in continuous listening mode.
 * Real-world offline wake word detection requires SDKs like Porcupine,
 * but this serves as a basic implementation.
 */
class VoiceCommandService : Service(), RecognitionListener {

    companion object {
        private const val TAG = "VoiceCommandService"
        private const val NOTIFICATION_ID = 4001
        private const val CHANNEL_ID = "droidbot_voice_channel"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting Voice Command Service...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        if (!isListening) {
            mainHandler.post { startListening() }
        }
        
        return START_STICKY
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition is not available on this device.")
            stopSelf()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(this)

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer?.startListening(recognizerIntent)
        isListening = true
        Log.i(TAG, "SpeechRecognizer started.")
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0].lowercase()
            Log.i(TAG, "Heard: $spokenText")
            
            // Check for wake word
            if (spokenText.contains("hey droidbot") || spokenText.contains("android bot") || spokenText.contains("droid bot")) {
                val command = spokenText
                    .replace("hey droidbot", "")
                    .replace("android bot", "")
                    .replace("droid bot", "")
                    .trim()
                
                if (command.isNotEmpty()) {
                    Log.i(TAG, "Wake word detected. Command: $command")
                    launchMainActivityWithTask(command)
                }
            }
        }
        
        // Immediately restart listening to create a continuous loop
        restartListening()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Optional: show partial results in UI if we want to
    }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        Log.d(TAG, "SpeechRecognizer error: $message Code: $error")
        
        // Restart on common non-fatal timeouts
        restartListening()
    }

    private fun restartListening() {
        mainHandler.postDelayed({
            if (isListening) {
                try {
                    val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    speechRecognizer?.startListening(recognizerIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restart listener", e)
                }
            }
        }, 500)
    }

    private fun launchMainActivityWithTask(taskDescription: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("VOICE_COMMAND_TASK", taskDescription)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroying Voice Command Service...")
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Unused RecognitionListener callbacks
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DroidBot Listening Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps DroidBot listening for voice commands"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DroidBot is listening")
            .setContentText("Say 'Hey DroidBot' to start the agent")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
