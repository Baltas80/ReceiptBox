package com.pagrey.receiptbox.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pagrey.receiptbox.ui.ReceiptBoxDesign

private val Ink = Color(0xFF17151F)
private val Muted = Color(0xFF6F6A78)
private val Brand = Color(0xFF5B4BDB)
private val BrandDark = Color(0xFF9B8CFF)
private val Teal = Color(0xFF0E9F92)
private val WarmBackground = Color(0xFFFAF9FC)
private val Surface = Color(0xFFFFFFFF)
private val SurfaceSoft = Color(0xFFF3F1F7)
private val Border = Color(0xFFE5E1EB)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E5FF),
    onPrimaryContainer = Color(0xFF25185F),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F5F1),
    onSecondaryContainer = Color(0xFF063C36),
    tertiary = Color(0xFFB77900),
    onTertiary = Color.White,
    background = WarmBackground,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = Muted,
    outline = Border,
    outlineVariant = Color(0xFFEDEAF1)
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = Color(0xFF30206D),
    primaryContainer = Color(0xFF40318A),
    onPrimaryContainer = Color(0xFFEAE5FF),
    secondary = Color(0xFF6DD7CC),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF111015),
    onBackground = Color(0xFFF1EEF5),
    surface = Color(0xFF18171D),
    onSurface = Color(0xFFF1EEF5),
    surfaceVariant = Color(0xFF25232B),
    onSurfaceVariant = Color(0xFFC7C1CD),
    outline = Color(0xFF45414C),
    outlineVariant = Color(0xFF332F39)
)

private val ReceiptShapes = Shapes(
    extraSmall = RoundedCornerShape(ReceiptBoxDesign.CORNER_COMPACT),
    small = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL),
    medium = RoundedCornerShape(ReceiptBoxDesign.CORNER_MEDIUM),
    large = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE),
    extraLarge = RoundedCornerShape(ReceiptBoxDesign.CORNER_EXTRA_LARGE)
)

private val ReceiptTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun ReceiptBoxTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ReceiptTypography,
        shapes = ReceiptShapes,
        content = content
    )
}
