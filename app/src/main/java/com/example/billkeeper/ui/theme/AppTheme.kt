package com.example.billkeeper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min

private val DefaultLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF0D3B16),
    secondary = Color(0xFF2E7D32),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFE0E5DF),
    onSurfaceVariant = Color(0xFF414941)
)

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390B),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFA5D6A7),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF181D18),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF3F4940),
    onSurfaceVariant = Color(0xFFBFC9BE)
)

@Composable
fun BillKeeperTheme(
    darkTheme: Boolean,
    themeSeedArgb: Int?,
    backgroundColorArgb: Int?,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
    val themedScheme = themeSeedArgb?.let { seed ->
        val primary = seed.toTone(if (darkTheme) 0.72f else 0.38f)
        val secondary = seed.toSecondaryTone(if (darkTheme) 0.70f else 0.40f)
        val primaryContainer = seed.toTone(if (darkTheme) 0.26f else 0.88f)
        baseScheme.copy(
            primary = primary,
            onPrimary = primary.readableContentColor(),
            primaryContainer = primaryContainer,
            onPrimaryContainer = primaryContainer.readableContentColor(),
            secondary = secondary,
            onSecondary = secondary.readableContentColor()
        )
    } ?: baseScheme
    val colorScheme = backgroundColorArgb?.let { argb ->
        val backgroundColor = Color(argb)
        themedScheme.copy(
            background = backgroundColor,
            onBackground = backgroundColor.readableContentColor()
        )
    } ?: themedScheme

    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun Int.toTone(lightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this, hsl)
    hsl[1] = max(hsl[1], 0.35f)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Int.toSecondaryTone(lightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this, hsl)
    hsl[0] = (hsl[0] + 24f) % 360f
    hsl[1] = min(max(hsl[1], 0.30f), 0.62f)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.readableContentColor(): Color =
    if (luminance() > 0.48f) Color(0xFF101410) else Color.White

val EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "娱乐", "住房", "日用", "医疗", "教育", "其他")
val INCOME_SOURCES = listOf("工资", "兼职", "投资", "理财", "红包", "报销", "其他")

val CATEGORY_COLORS = mapOf(
    "餐饮" to Color(0xFFE57373),
    "交通" to Color(0xFF64B5F6),
    "购物" to Color(0xFFFFB74D),
    "娱乐" to Color(0xFFBA68C8),
    "住房" to Color(0xFF4DB6AC),
    "日用" to Color(0xFFAED581),
    "医疗" to Color(0xFFBCAAA4),
    "教育" to Color(0xFFFFD54F),
    "其他" to Color(0xFF90A4AE)
)

val INCOME_COLORS = mapOf(
    "工资" to Color(0xFF43A047),
    "兼职" to Color(0xFF26A69A),
    "投资" to Color(0xFF5C6BC0),
    "理财" to Color(0xFFAB47BC),
    "红包" to Color(0xFFEF5350),
    "报销" to Color(0xFFFFA726),
    "其他" to Color(0xFF78909C)
)
