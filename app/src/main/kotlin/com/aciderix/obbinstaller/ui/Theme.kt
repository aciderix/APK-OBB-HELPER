package com.aciderix.obbinstaller.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object HubColors {
    val Background = Color(0xFF070E11)
    val Surface = Color(0xFF0E1A1F)
    val SurfaceElevated = Color(0xFF14252B)
    val SurfaceMuted = Color(0xFF0B161A)

    val Primary = Color(0xFF1AE6B0)
    val PrimaryDark = Color(0xFF0D7B5F)
    val Accent = Color(0xFF49E0D9)
    val AccentSoft = Color(0xFF1F8983)

    val Border = Color(0xFF1E3A40)
    val BorderActive = Color(0xFF2BCBA8)
    val BorderGlow = Color(0xFF1AE6B0)

    val TextPrimary = Color(0xFFE8F4F2)
    val TextSecondary = Color(0xFF8DA3A6)
    val TextMuted = Color(0xFF566366)
    val TextHint = Color(0xFF3F5054)

    val Danger = Color(0xFFFF6E6E)
    val Warn = Color(0xFFFFC857)

    val InstallGradient = Brush.horizontalGradient(
        listOf(Color(0xFF14B98A), Color(0xFF1AE6B0), Color(0xFF49E0D9))
    )
    val GlowGradient = Brush.verticalGradient(
        listOf(Color(0x331AE6B0), Color(0x111AE6B0), Color(0x00000000))
    )
}

private val HubTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HubColors.TextPrimary),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = HubColors.TextPrimary),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HubColors.TextPrimary),
    bodyLarge = TextStyle(fontSize = 15.sp, color = HubColors.TextPrimary),
    bodyMedium = TextStyle(fontSize = 13.sp, color = HubColors.TextSecondary),
    bodySmall = TextStyle(fontSize = 12.sp, color = HubColors.TextSecondary),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HubColors.TextPrimary),
    labelMedium = TextStyle(fontSize = 12.sp, color = HubColors.TextSecondary),
    labelSmall = TextStyle(fontSize = 11.sp, color = HubColors.TextMuted)
)

private val HubScheme = darkColorScheme(
    primary = HubColors.Primary,
    onPrimary = Color(0xFF002418),
    primaryContainer = HubColors.PrimaryDark,
    onPrimaryContainer = HubColors.Primary,
    secondary = HubColors.Accent,
    onSecondary = Color(0xFF002A2A),
    background = HubColors.Background,
    onBackground = HubColors.TextPrimary,
    surface = HubColors.Surface,
    onSurface = HubColors.TextPrimary,
    surfaceVariant = HubColors.SurfaceElevated,
    onSurfaceVariant = HubColors.TextSecondary,
    outline = HubColors.Border,
    error = HubColors.Danger
)

@Composable
fun ObbInstallerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HubScheme,
        typography = HubTypography,
        content = content
    )
}
