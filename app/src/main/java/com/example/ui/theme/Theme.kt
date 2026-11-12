package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CalculatorColors(
  val displayBackground: Color,
  val displayBorder: Color,
  val numberKey: Color,
  val numberKeyContent: Color,
  val operatorKey: Color,
  val operatorKeyContent: Color,
  val scientificKey: Color,
  val scientificKeyContent: Color,
  val clearKey: Color,
  val clearKeyContent: Color,
  val equalsKey: Color,
  val equalsKeyContent: Color,
  val subtleText: Color,
)

val LocalCalculatorColors = staticCompositionLocalOf {
  LightCalculatorColors
}

private val LightCalculatorColors =
  CalculatorColors(
    displayBackground = LightDisplaySurface,
    displayBorder = LightOutline,
    numberKey = LightKeyNumber,
    numberKeyContent = LightKeyNumberText,
    operatorKey = LightKeyOperator,
    operatorKeyContent = LightKeyOperatorText,
    scientificKey = LightKeyScientific,
    scientificKeyContent = LightKeyScientificText,
    clearKey = LightKeyClear,
    clearKeyContent = LightKeyClearText,
    equalsKey = BrandOrange,
    equalsKeyContent = Color.White,
    subtleText = LightTextSecondary,
  )

private val DarkCalculatorColors =
  CalculatorColors(
    displayBackground = DarkDisplaySurface,
    displayBorder = DarkOutline,
    numberKey = DarkKeyNumber,
    numberKeyContent = DarkKeyNumberText,
    operatorKey = DarkKeyOperator,
    operatorKeyContent = DarkKeyOperatorText,
    scientificKey = DarkKeyScientific,
    scientificKeyContent = DarkKeyScientificText,
    clearKey = DarkKeyClear,
    clearKeyContent = DarkKeyClearText,
    equalsKey = BrandOrange,
    equalsKeyContent = Color.White,
    subtleText = DarkTextSecondary,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = LightKeyOperator,
    onPrimaryContainer = LightKeyOperatorText,
    secondary = BrandNavy,
    onSecondary = Color.White,
    secondaryContainer = LightKeyScientific,
    onSecondaryContainer = LightKeyScientificText,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightDisplaySurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightKeyScientific,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = LightKeyClearText,
    errorContainer = LightKeyClear,
    onErrorContainer = LightKeyClearText,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = DarkKeyOperator,
    onPrimaryContainer = DarkKeyOperatorText,
    secondary = Color(0xFF93C5FD),
    onSecondary = BrandNavyDeep,
    secondaryContainer = DarkKeyScientific,
    onSecondaryContainer = DarkKeyScientificText,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkDisplaySurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkKeyNumber,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = DarkKeyClearText,
    errorContainer = DarkKeyClear,
    onErrorContainer = DarkKeyClearText,
  )

@Composable
fun SmartCalcTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val calcColors = if (darkTheme) DarkCalculatorColors else LightCalculatorColors

  CompositionLocalProvider(LocalCalculatorColors provides calcColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = SmartCalcTypography,
      content = content,
    )
  }
}
