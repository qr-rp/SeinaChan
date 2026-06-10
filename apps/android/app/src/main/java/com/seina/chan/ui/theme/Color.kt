package com.seina.chan.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// === Brand / Primary ===
val Primary = Color(0xFFCC785C)
val PrimaryHover = Color(0xFFB56A51)
val PrimaryActive = Color(0xFFa9583e)
val PrimaryDisabled = Color(0xFFe6dfd8)

// === Canvas / Background ===
val Canvas = Color(0xFFFAF9F5)

// === Ink / Text ===
val Ink = Color(0xFF141413)
val InkLight = Color(0xFF6c6a64)
val Body = Color(0xFF3d3d3a)
val BodyStrong = Color(0xFF252523)
val Muted = Color(0xFF6c6a64)
val MutedSoft = Color(0xFF8e8b82)

// === Hairline / Borders ===
val Hairline = Color(0xFFe6dfd8)

// === Surfaces ===
val SurfaceCard = Color(0xFFefe9de)
val SurfaceCreamStrong = Color(0xFFe8e0d2)
val SurfaceCoralLight = Color(0xFFFDF4F1)
val SurfaceCoral = Color(0xFFF8E8E2)
val SurfaceCoralStrong = Color(0xFFF5DBD2)
val SurfaceDark = Color(0xFF181715)
val SurfaceNavy = Color(0xFF1A1D2E)
val SurfaceNavyLight = Color(0xFF2A2E42)
val BorderNavy = Color(0xFF3A3F5C)

// === Semantic ===
val Success = Color(0xFF5db872)
val ErrorColor = Color(0xFFc64545)

// === Code Block ===
val CodeBg = Color(0xFF181715)
val CodeText = Canvas

// === Light Color Scheme ===
val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDisabled,
    onPrimaryContainer = Ink,
    secondary = SurfaceCreamStrong,
    onSecondary = Ink,
    secondaryContainer = SurfaceCoralLight,
    onSecondaryContainer = Ink,
    tertiary = SurfaceNavy,
    onTertiary = Color.White,
    tertiaryContainer = SurfaceNavyLight,
    onTertiaryContainer = Color.White,
    background = Canvas,
    onBackground = Ink,
    surface = Canvas,
    onSurface = Ink,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Muted,
    error = ErrorColor,
    onError = Color.White,
    outline = Hairline,
    scrim = SurfaceDark.copy(alpha = 0.4f),
    inverseSurface = SurfaceDark,
    inverseOnSurface = CodeText,
    inversePrimary = PrimaryHover,
)

// === Dark Color Scheme (for code blocks & dark surfaces) ===
val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = SurfaceCoralStrong,
    onPrimaryContainer = Ink,
    secondary = SurfaceNavy,
    onSecondary = Color.White,
    secondaryContainer = SurfaceNavyLight,
    onSecondaryContainer = Color.White,
    tertiary = SurfaceCoral,
    onTertiary = Ink,
    tertiaryContainer = SurfaceCoralLight,
    onTertiaryContainer = Ink,
    background = SurfaceDark,
    onBackground = CodeText,
    surface = SurfaceNavy,
    onSurface = Color.White,
    surfaceVariant = SurfaceNavyLight,
    onSurfaceVariant = CodeText.copy(alpha = 0.7f),
    error = Primary,
    onError = Color.White,
    outline = BorderNavy,
    scrim = Color.Black.copy(alpha = 0.6f),
    inverseSurface = Canvas,
    inverseOnSurface = Ink,
    inversePrimary = PrimaryHover,
)
