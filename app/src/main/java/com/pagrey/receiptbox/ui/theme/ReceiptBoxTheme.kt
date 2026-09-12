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
private val BrandDark = Color(0xFFB2A6FF)
private val Teal = Color(0xFF0E9F92)
private val WarmBackground = Color(0xFFF4F1FA)
private val Surface = Color(0xFFFFFFFF)
private val SurfaceSoft = Color(0xFFEDE8F4)
private val Border = Color(0xFFDCD5E7)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1DBFF),
    onPrimaryContainer = Color(0xFF211650),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F2ED),
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
    outlineVariant = Color(0xFFE4DEEB)
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = Color(0xFF30206D),
    primaryContainer = Color(0xFF40318A),
    onPrimaryContainer = Color(0xFFEAE5FF),
    secondary = Color(0xFF72D9CF),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF100E17),
    onBackground = Color(0xFFF4F0FA),
    surface = Color(0xFF191622),
    onSurface = Color(0xFFF4F0FA),
    surfaceVariant = Color(0xFF292436),
    onSurfaceVariant = Color(0xFFC9C1D2),
    outline = Color(0xFF4A4254),
    outlineVariant = Color(0xFF393143)
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
