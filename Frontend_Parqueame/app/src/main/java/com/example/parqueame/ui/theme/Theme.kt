package com.example.parqueame.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.parqueame.R

// --- Fuentes ---
val rtlRomman = FontFamily(
    Font(R.font.rtl_romman_regular)
)

val dmSans = FontFamily(
    Font(R.font.dm_sans_regular, weight = FontWeight.Normal),
    Font(R.font.dm_sans_medium,  weight = FontWeight.Medium),
)

// --- Paleta de colores personalizada ---
val PrimaryBlue = Color(0xFF00A1FF)
val PrimaryDarkBlue = Color(0xFF003099)

// --- Gradientes específicos para login ---
fun loginBackgroundGradient() = Brush.verticalGradient(
    colors = listOf(
        PrimaryBlue,
        PrimaryDarkBlue
    ),
    startY = 0f,
    endY = 1000f
)

fun loginTopGradientColor() = Color.Transparent

// --- Colores para tema general ---
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryDarkBlue,
    tertiary = Color(0xFFBB86FC),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryDarkBlue,
    tertiary = Color(0xFF6200EE),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// --- Tipografía ---
val base = Typography()

val AppTypography = base.copy(
    displayLarge  = base.displayLarge.copy(fontFamily = DmSans),
    displayMedium = base.displayMedium.copy(fontFamily = DmSans),
    displaySmall  = base.displaySmall.copy(fontFamily = DmSans),
    headlineLarge = base.headlineLarge.copy(fontFamily = RtlRomman, fontSize = 40.sp),
    headlineMedium= base.headlineMedium.copy(fontFamily = DmSans),
    headlineSmall = base.headlineSmall.copy(fontFamily = DmSans),
    titleLarge    = base.titleLarge.copy(fontFamily = DmSans),
    titleMedium   = base.titleMedium.copy(fontFamily = DmSans),
    titleSmall    = base.titleSmall.copy(fontFamily = DmSans),
    bodyLarge     = base.bodyLarge.copy(fontFamily = DmSans),
    bodyMedium    = base.bodyMedium.copy(fontFamily = DmSans),
    bodySmall     = base.bodySmall.copy(fontFamily = DmSans),
    labelLarge    = base.labelLarge.copy(fontFamily = DmSans),
    labelMedium   = base.labelMedium.copy(fontFamily = DmSans),
    labelSmall    = base.labelSmall.copy(fontFamily = DmSans),
)

// --- Tema principal ---
@Composable
fun ParqueameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        //darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
