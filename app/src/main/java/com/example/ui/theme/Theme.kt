package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val label: String) {
  DARK("Dark"),
  LIGHT("Light"),
  SYSTEM("System"),
}

private val DarkShoppingColorScheme =
  darkColorScheme(
    primary = AmberGold,
    onPrimary = OnAmberGold,
    primaryContainer = AmberGoldContainer,
    onPrimaryContainer = OnAmberGoldContainer,
    secondary = TealCyan,
    onSecondary = OnTealCyan,
    secondaryContainer = TealCyanContainer,
    onSecondaryContainer = OnTealCyanContainer,
    tertiary = LimeNeon,
    onTertiary = Color(0xFF0D1F00),
    tertiaryContainer = Color(0xFF1F3805),
    onTertiaryContainer = Color(0xFFD9F99D),
    background = DarkShopBackground,
    onBackground = DarkShopOnSurface,
    surface = DarkShopSurface,
    onSurface = DarkShopOnSurface,
    surfaceVariant = DarkShopSurfaceElevated,
    onSurfaceVariant = DarkShopOnSurfaceVariant,
    outline = DarkShopBorder,
    error = AlertRose,
  )

private val LightShoppingColorScheme =
  lightColorScheme(
    primary = LightAmberPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = LightTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = DealGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF052E16),
    background = LightShopBackground,
    onBackground = LightShopOnSurface,
    surface = LightShopSurface,
    onSurface = LightShopOnSurface,
    surfaceVariant = LightShopSurfaceVariant,
    onSurfaceVariant = LightShopOnSurfaceVariant,
    outline = LightShopOutline,
    error = AlertRose,
  )

@Composable
fun SmartBuyTheme(
  themeMode: AppThemeMode = AppThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val useDark =
    when (themeMode) {
      AppThemeMode.DARK -> true
      AppThemeMode.LIGHT -> false
      AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
  val colorScheme = if (useDark) DarkShoppingColorScheme else LightShoppingColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = SmartBuyTypography, content = content)
}
