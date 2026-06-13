package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanNeon,
    secondary = BlueNeon,
    tertiary = OrangeNeon,
    background = CyberBackground,
    surface = CyberSurface,
    surfaceVariant = CyberSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = BlueNeon,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
  )

private val LightColorScheme = DarkColorScheme // Default both light/dark to beautiful game dark mode representatively

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force gorgeous dark gaming UI by default
  dynamicColor: Boolean = false, // Disable dynamic colors so our intentional game aesthetic isn't color-shifted
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
