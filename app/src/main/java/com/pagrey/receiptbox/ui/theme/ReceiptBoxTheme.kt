package com.pagrey.receiptbox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val Brand = Color(0xFF2563EB)
private val LightColors = lightColorScheme(primary = Brand, secondary = Color(0xFF475569))
private val DarkColors = darkColorScheme(primary = Color(0xFF8AB4FF), secondary = Color(0xFFCBD5E1))

private val ReceiptTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun ReceiptBoxTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ReceiptTypography,
        content = content
    )
}
