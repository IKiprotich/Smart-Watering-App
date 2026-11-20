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

private val Leaf = Color(0xFF245646)
private val LeafDark = Color(0xFFAFDDC1)
private val Canvas = Color(0xFFF8FAF4)
private val Ink = Color(0xFF17211B)
private val Muted = Color(0xFF657067)

private val Light = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEEDC),
    onPrimaryContainer = Color(0xFF153A29),
    secondary = Color(0xFFE5BA71),
    background = Canvas,
    surface = Color.White,
    surfaceVariant = Color(0xFFEAF0E5),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outlineVariant = Color(0xFFD8DED6),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val Dark = darkColorScheme(
    primary = LeafDark,
    onPrimary = Color(0xFF102118),
    primaryContainer = Color(0xFF293F32),
    onPrimaryContainer = Color(0xFFDBF1E0),
    secondary = Color(0xFFF0BD69),
    background = Color(0xFF101611),
    surface = Color(0xFF18231C),
    surfaceVariant = Color(0xFF243129),
    onBackground = Color(0xFFE8F0EA),
    onSurface = Color(0xFFE8F0EA),
    onSurfaceVariant = Color(0xFFB3C0B7),
    outlineVariant = Color(0xFF3C4B41),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val GardenShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SmartWateringTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val useDarkTheme = darkTheme ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDarkTheme) Dark else Light,
        typography = gardenTypography(),
        shapes = GardenShapes,
        content = content
    )
}

private fun gardenTypography() = Typography(
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
