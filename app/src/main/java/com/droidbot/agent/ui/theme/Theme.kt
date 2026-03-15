package com.droidbot.agent.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ══════════════════════════════════════════════════════
// DroidBot Color Palette — Cyber-Synthetic Dark Theme
// ══════════════════════════════════════════════════════

private val DroidPrimary = Color(0xFF00E5FF)       // Cyan — primary actions
private val DroidSecondary = Color(0xFF7C4DFF)      // Deep purple — accents
private val DroidTertiary = Color(0xFF00E676)       // Green — success/active
private val DroidError = Color(0xFFFF5252)          // Red — errors

private val DroidBackground = Color(0xFF0A0E14)     // Near-black — canvas
private val DroidSurface = Color(0xFF121820)        // Dark card surface
private val DroidSurfaceVariant = Color(0xFF1A2130) // Elevated surface
private val DroidOnPrimary = Color(0xFF001F26)
private val DroidOnBackground = Color(0xFFE0E0E0)
private val DroidOnSurface = Color(0xFFCFD8DC)
private val DroidOnSurfaceVariant = Color(0xFF90A4AE)
private val DroidOutline = Color(0xFF2A3540)

private val DroidDarkColorScheme = darkColorScheme(
    primary = DroidPrimary,
    onPrimary = DroidOnPrimary,
    primaryContainer = Color(0xFF004D5B),
    onPrimaryContainer = DroidPrimary,
    secondary = DroidSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF3A1D8E),
    onSecondaryContainer = Color(0xFFD1C4E9),
    tertiary = DroidTertiary,
    onTertiary = Color(0xFF003300),
    error = DroidError,
    onError = Color(0xFF690005),
    background = DroidBackground,
    onBackground = DroidOnBackground,
    surface = DroidSurface,
    onSurface = DroidOnSurface,
    surfaceVariant = DroidSurfaceVariant,
    onSurfaceVariant = DroidOnSurfaceVariant,
    outline = DroidOutline
)

@Composable
fun DroidBotTheme(content: @Composable () -> Unit) {
    val colorScheme = DroidDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DroidBackground.toArgb()
            window.navigationBarColor = DroidBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // Uses default Material3 typography
        content = content
    )
}
