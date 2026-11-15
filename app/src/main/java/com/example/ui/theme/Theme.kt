package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CivicNavy,
    onPrimary = Color.White,
    primaryContainer = CivicNavyContainer,
    onPrimaryContainer = OnCivicNavyContainer,
    secondary = SafetyAmber,
    onSecondary = Color.White,
    secondaryContainer = SafetyAmberContainer,
    onSecondaryContainer = OnSafetyAmberContainer,
    tertiary = SlateTeal,
    onTertiary = Color.White,
    tertiaryContainer = SlateTealContainer,
    onTertiaryContainer = OnSlateTealContainer,
    error = FailCrimson,
    onError = Color.White,
    errorContainer = FailCrimsonSurface,
    onErrorContainer = OnFailCrimsonSurface,
    background = SurfaceBackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceCardLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = CivicNavyDark,
    primaryContainer = CivicNavyLight,
    onPrimaryContainer = Color.White,
    secondary = SafetyAmberLight,
    onSecondary = Color(0xFF2B1300),
    secondaryContainer = Color(0xFF5A3200),
    onSecondaryContainer = SafetyAmberContainer,
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF00201D),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = SlateTealContainer,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = SurfaceBackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceCardDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

private val PlaySafeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun PlaySafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PlaySafeTypography,
        shapes = PlaySafeShapes,
        content = content,
    )
}
