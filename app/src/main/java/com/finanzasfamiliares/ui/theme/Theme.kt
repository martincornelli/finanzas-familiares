package com.finanzasfamiliares.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green700  = Color(0xFF2E7D32)
val Green100  = Color(0xFFC8E6C9)
val Yellow700 = Color(0xFFF9A825)
val Yellow100 = Color(0xFFFFF9C4)
val Red700    = Color(0xFFC62828)
val Red100    = Color(0xFFFFCDD2)

data class MarginColors(val background: Color, val content: Color)

fun marginColors(amount: Double, greenThreshold: Double = 20000.0, yellowThreshold: Double = 5000.0): MarginColors = when {
    amount >= greenThreshold  -> MarginColors(Green100, Green700)
    amount >= yellowThreshold -> MarginColors(Yellow100, Yellow700)
    else                      -> MarginColors(Red100, Red700)
}

private val LightColorScheme = lightColorScheme(
    primary     = Color(0xFF1565C0),
    onPrimary   = Color.White,
    surface     = Color(0xFFF5F7FA),
    onSurface   = Color(0xFF1C1B1F),
    background  = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F)
)

@Composable
fun FinanzasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography(),
        content     = content
    )
}
