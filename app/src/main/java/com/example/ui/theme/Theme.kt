package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NoirColorScheme = darkColorScheme(
  primary = InkPureWhite,
  onPrimary = InkPureBlack,
  primaryContainer = NoirSurfaceElevated,
  onPrimaryContainer = InkPureWhite,
  secondary = InkLightGray,
  onSecondary = InkPureBlack,
  secondaryContainer = NoirCard,
  onSecondaryContainer = InkPureWhite,
  tertiary = InkMediumGray,
  onTertiary = InkPureBlack,
  background = NoirBlack,
  onBackground = InkPureWhite,
  surface = NoirSurface,
  onSurface = InkPureWhite,
  surfaceVariant = NoirCard,
  onSurfaceVariant = InkLightGray,
  outline = NoirBorder,
  outlineVariant = NoirBorderLight,
)

@Composable
fun MonoIconTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = NoirColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) = MonoIconTheme(content = content)

