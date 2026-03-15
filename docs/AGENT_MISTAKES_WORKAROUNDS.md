# DroidBot Agent Mistakes & Workarounds Log

This document records the mistakes made by AI agents during modifications, the challenges encountered, and the workarounds developed. 
**Future agents MUST consult this file to avoid repeating failures.**

---

## 💥 Mistake: Invalid Gemini Model Names
- **Problem**: The agent generated code specifying `MODEL_NAME = "gemini-1.5-flash-latest"`. When the ReAct loop initiated, this caused an HTTP 404 Model Not Found error, crashing the `NavigationBrain`.
- **Challenge**: The agent had a hardcoded illusion of a model name. Additionally, there was no fallback.
- **Workaround/Fix**: The user explicitly instructed the use of `gemini-2.5-flash` and `gemini-2.5-flash-lite`. The `CloudInference.kt` file was updated to use these exact strings, with a `try/catch` block explicitly falling back to the lite model if the primary fails. Error logs were piped to the `ActionLog` to ensure the user gets visual feedback when an API call dies.

## 💥 Mistake: Forcing the "Home Screen" on Task Start
- **Problem**: To ensure a "clean slate", `MainActivity` forced the phone to execute `GLOBAL_ACTION_HOME` every time a new task started. The user complained the bot would just open an app, navigate away to Home, and freeze.
- **Challenge**: The LLM needs a guaranteed starting point, but forcing Home breaks contextual tasks (like "reply to the email I am currently reading").
- **Workaround/Fix**: Removed the forced Home Screen routing in `MainActivity`. The agent now analyzes whatever screen is currently active and begins its ReAct loop from there.

## 💥 Mistake: Abstract Class Compilation Error in Voice Service
- **Problem**: When implementing `RecognitionListener` in `VoiceCommandService.kt`, the agent missed the `onEndOfSpeech` abstract function. This caused a `compileDebugKotlin` failure in Gradle with the error `Class 'VoiceCommandService' is not abstract and does not implement abstract member 'onEndOfSpeech'`.
- **Challenge**: Android's `SpeechRecognizer` API has numerous callbacks that must all be overridden. The agent truncated the code.
- **Workaround/Fix**: Ran `.\gradlew :app:compileDebugKotlin` and caught the error log manually in `build_error.log`. Inserted the missing `override fun onEndOfSpeech() {}` callback to fix the compilation error. 

## 💥 Mistake: Android 14+ Foreground Service Crash
- **Problem**: The `VoiceCommandService` crashed on startup with an `IllegalStateException` on Android 14+.
- **Challenge**: Android 14+ requires `FOREGROUND_SERVICE_TYPE_MICROPHONE` for any service accessing the microphone in the background.
- **Workaround/Fix**: Appended `foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE` to the notification builder and `AndroidManifest.xml`.

## 💥 Mistake: API Keys Pushed to Git
- **Problem**: A standard `git add .` command included `local.properties` (which contained `GEMINI_API_KEY`) because the `.gitignore` was not formatted correctly.
- **Challenge**: The project was not a Git repository initially, so the agent created `.gitignore` but didn't verify if it excluded the file properly before the initial commit.
- **Workaround/Fix**: Urgently executed `git rm --cached local.properties`, fixed the `.gitignore` file using generic patterns, amended the commit via `git commit --amend`, and verified the Git index before final push.
