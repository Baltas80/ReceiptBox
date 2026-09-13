package com.pagrey.receiptbox.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.pagrey.receiptbox.ui.ReceiptBoxDesign

private val Ink = Color(0xFF151321)
private val Muted = Color(0xFF625D70)
private val Brand = Color(0xFF5B45E6)
private val BrandDark = Color(0xFFB8ADFF)
private val Teal = Color(0xFF12A79A)
private val WarmBackground = Color(0xFFEDE9F6)
private val Surface = Color(0xFFF9F7FD)
private val SurfaceSoft = Color(0xFFE3DDED)
private val Border = Color(0xFFD2C9E0)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCD4FF),
    onPrimaryContainer = Color(0xFF20134F),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFE9),
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
    outlineVariant = Color(0xFFDAD2E5)
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = Color(0xFF30206D),
    primaryContainer = Color(0xFF40318A),
    onPrimaryContainer = Color(0xFFEAE5FF),
    secondary = Color(0xFF72D9CF),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF0D0B14),
    onBackground = Color(0xFFF7F3FC),
    surface = Color(0xFF17141F),
    onSurface = Color(0xFFF7F3FC),
    surfaceVariant = Color(0xFF272230),
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
        displaySmall = displaySmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.7).sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        ),
        headlineSmall = headlineSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp
        ),
        headlineMedium = headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.45).sp
        ),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, letterSpacing = (-0.15).sp),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.05).sp),
        labelLarge = labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)),
        bodyMedium = bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
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
