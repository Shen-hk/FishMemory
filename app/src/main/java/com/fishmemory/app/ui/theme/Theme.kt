package com.fishmemory.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
//拓展建议:自定义颜色  自定义字体
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1E88E5),      // 主色保持不变
    secondary = Color(0xFF03DAC6),    // 次要色保持不变
    tertiary = Color(0xFF3700B3),     // 第三色保持不变
    background = Color(0xFF121212),   // 背景：深灰色
    surface = Color(0xFF1E1E1E),      // 表面：稍亮的深灰
    onPrimary = Color.White,          // 主色上的文字：白色
    onSecondary = Color.Black,        // 次要色上的文字：黑色
    onBackground = Color.White,       // 背景上的文字：白色
    onSurface = Color.White           // 表面上的文字：白色
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E88E5),      // 主色：蓝色
    secondary = Color(0xFF03DAC6),    // 次要色：青色
    tertiary = Color(0xFF3700B3),     // 第三色：深蓝色
    background = Color(0xFFFFFFFF),   // 背景：白色
    surface = Color(0xFFFFFFFF),      // 表面：白色
    onPrimary = Color.White,          // 主色上的文字：白色
    onSecondary = Color.Black,        // 次要色上的文字：黑色
    onBackground = Color.Black,       // 背景上的文字：黑色
    onSurface = Color.Black           // 表面上的文字：黑色
)
private val FishMemoryTypography = Typography(
    displayLarge = TextStyle(                //显示文字
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp
    ),
    headlineLarge = TextStyle(              //标题文字head
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(             //标题文字
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(                  //正文
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(                 //标签
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

@Composable
fun FishMemoryTheme(
    darkTheme: Boolean = false,  // 深色模式开关pp
    content: @Composable () -> Unit  // 主题内容
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,      // 颜色方案
        typography = FishMemoryTypography, // 文字排版
        content = content               // 应用内容
    )
}