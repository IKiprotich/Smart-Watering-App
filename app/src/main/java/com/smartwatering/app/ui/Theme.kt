package com.smartwatering.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Rain = Color(0xFF24547A)
private val RainDark = Color(0xFFADD5F2)
private val Canvas = Color(0xFFF5F6F2)
private val Ink = Color(0xFF182825)
private val Muted = Color(0xFF52615D)

private val Light = lightColorScheme(
    primary = Rain,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEEAF4),
    onPrimaryContainer = Color(0xFF103653),
    secondary = Color(0xFF45634B),
    secondaryContainer = Color(0xFFDFEAD6),
    onSecondaryContainer = Color(0xFF26472D),
    surfaceContainer = Color(0xFFE9EEE9),
    surfaceContainerHigh = Color(0xFFE5EAE5),
    background = Canvas,
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EDE6),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outlineVariant = Color(0xFFD7DFD9),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val Dark = darkColorScheme(
    primary = RainDark,
    onPrimary = Color(0xFF102D43),
    primaryContainer = Color(0xFF20394B),
    onPrimaryContainer = Color(0xFFD9EDFD),
    secondary = Color(0xFFB9D4B4),
    secondaryContainer = Color(0xFF293B2E),
    onSecondaryContainer = Color(0xFFD2E7CC),
    surfaceContainer = Color(0xFF202D2A),
    surfaceContainerHigh = Color(0xFF263430),
    background = Color(0xFF111C1B),
    surface = Color(0xFF182522),
    surfaceVariant = Color(0xFF22302C),
    onBackground = Color(0xFFE8F0EA),
    onSurface = Color(0xFFE8F0EA),
    onSurfaceVariant = Color(0xFFB3C0B7),
    outlineVariant = Color(0xFF374641),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val GardenShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SmartWateringTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val useDarkTheme = darkTheme ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDarkTheme) Dark else Light,
        typography = GardenTypography,
        shapes = GardenShapes,
        content = content
    )
}

private val GardenTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 40.sp,
        lineHeight = 46.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
        fontSize = 35.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-.6).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-.3).sp
    ),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)
