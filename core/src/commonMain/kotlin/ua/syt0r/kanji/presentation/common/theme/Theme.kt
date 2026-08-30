package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import ua.syt0r.kanji.core.theme_manager.LocalThemeManager
import ua.syt0r.kanji.presentation.common.resources.string.LocalStrings
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.common.sound.LocalPracticeSounds
import ua.syt0r.kanji.presentation.common.sound.rememberPracticeSoundPlayer
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation

// 明/暗的固定背景与容器表面（保证永不黑屏；主色系由 seed 动态生成并随 BrandSeedColor 联动）
private val LightNeutrals = LightSurfaceColors(
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDD8), onSurfaceVariant = Color(0xFF53433F),
    outline = Color(0xFF85736E), outlineVariant = Color(0xFFD8C2BC),
    inverseSurface = Color(0xFF362F2E), inverseOnSurface = Color(0xFFFBEEEC),
    surfaceBright = Color(0xFFFFFBFF), surfaceDim = Color(0xFFE6DEDC),
    surfaceContainer = Color(0xFFF3EDEB), surfaceContainerHigh = Color(0xFFEDE4E1),
    surfaceContainerHighest = Color(0xFFE7DEDB), surfaceContainerLow = Color(0xFFFFFCFA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

private val DarkNeutrals = LightSurfaceColors(
    background = Color(0xFF201A19), onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF201A19), onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF53433F), onSurfaceVariant = Color(0xFFD8C2BC),
    outline = Color(0xFFA08C87), outlineVariant = Color(0xFF53433F),
    inverseSurface = Color(0xFFEDE0DE), inverseOnSurface = Color(0xFF362F2E),
    surfaceBright = Color(0xFF3B3433), surfaceDim = Color(0xFF201A19),
    surfaceContainer = Color(0xFF2D2524), surfaceContainerHigh = Color(0xFF373030),
    surfaceContainerHighest = Color(0xFF423A39), surfaceContainerLow = Color(0xFF251D1C),
    surfaceContainerLowest = Color(0xFF1C1413),
)

private val AmoledNeutrals = LightSurfaceColors(
    background = Color(0xFF000000), onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF000000), onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF2A2A2A), onSurfaceVariant = Color(0xFFD8C2BC),
    outline = Color(0xFFA08C87), outlineVariant = Color(0xFF2A2A2A),
    inverseSurface = Color(0xFFEDE0DE), inverseOnSurface = Color(0xFF362F2E),
    surfaceBright = Color(0xFF121212), surfaceDim = Color(0xFF121212),
    surfaceContainer = Color(0xFF121212), surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF1F1F1F), surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainerLowest = Color(0xFF000000),
)

private class LightSurfaceColors(
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val outline: Color, val outlineVariant: Color,
    val inverseSurface: Color, val inverseOnSurface: Color,
    val surfaceBright: Color, val surfaceDim: Color,
    val surfaceContainer: Color, val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color, val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
)

class ExtraColorsScheme(
    val link: Color,
    val success: Color,
    val pending: Color,
    val due: Color,
    val new: Color
)

val LightExtraColorScheme = ExtraColorsScheme(
    link = Color(0xFF0054D7),
    success = Color(0xFF2E7D32),
    pending = Color(0xFF7A5C55),
    due = Color(0xFFF9A825),
    new = Color(0xFF0277BD)
)

val DarkExtraColorScheme = ExtraColorsScheme(
    link = Color(0xFF5CA9E6),
    success = Color(0xFF66BB6A),
    pending = Color(0xFFE0C4BD),
    due = Color(0xFFFFC94D),
    new = Color(0xFF4FC3F7)
)

val LocalExtraColors = compositionLocalOf { LightExtraColorScheme }

val MaterialTheme.extraColorScheme: ExtraColorsScheme
    @Composable
    get() = LocalExtraColors.current

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useAmoledTheme: Boolean = false,
    orientation: Orientation = Orientation.Portrait,
    content: @Composable () -> Unit
) {
    val practiceSounds = rememberPracticeSoundPlayer()

    val isDark = useDarkTheme || useAmoledTheme

    // 用户自定义主色（设置里输入的色号）；未设置时回退品牌色
    val seedColor = LocalThemeManager.current.currentCustomSeedColor.value ?: BrandSeedColor

    // 由品牌 seed 生成的核心色（primary/secondary/tertiary/error）—— 随自定义色/品牌色动态联动
    val seeded = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = false
    )

    val neutrals = when {
        useAmoledTheme -> AmoledNeutrals
        isDark -> DarkNeutrals
        else -> LightNeutrals
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = seeded.primary, onPrimary = seeded.onPrimary,
            primaryContainer = seeded.primaryContainer, onPrimaryContainer = seeded.onPrimaryContainer,
            secondary = seeded.secondary, onSecondary = seeded.onSecondary,
            secondaryContainer = seeded.secondaryContainer, onSecondaryContainer = seeded.onSecondaryContainer,
            tertiary = seeded.tertiary, onTertiary = seeded.onTertiary,
            tertiaryContainer = seeded.tertiaryContainer, onTertiaryContainer = seeded.onTertiaryContainer,
            error = seeded.error, onError = seeded.onError,
            errorContainer = seeded.errorContainer, onErrorContainer = seeded.onErrorContainer,
            background = neutrals.background, onBackground = neutrals.onBackground,
            surface = neutrals.surface, onSurface = neutrals.onSurface,
            surfaceVariant = neutrals.surfaceVariant, onSurfaceVariant = neutrals.onSurfaceVariant,
            surfaceTint = seeded.primary,
            inverseSurface = neutrals.inverseSurface, inverseOnSurface = neutrals.inverseOnSurface,
            inversePrimary = seeded.primary,
            outline = neutrals.outline, outlineVariant = neutrals.outlineVariant,
            scrim = Color(0xFF000000),
            surfaceBright = neutrals.surfaceBright, surfaceDim = neutrals.surfaceDim,
            surfaceContainer = neutrals.surfaceContainer,
            surfaceContainerHigh = neutrals.surfaceContainerHigh,
            surfaceContainerHighest = neutrals.surfaceContainerHighest,
            surfaceContainerLow = neutrals.surfaceContainerLow,
            surfaceContainerLowest = neutrals.surfaceContainerLowest,
        )
    } else {
        lightColorScheme(
            primary = seeded.primary, onPrimary = seeded.onPrimary,
            primaryContainer = seeded.primaryContainer, onPrimaryContainer = seeded.onPrimaryContainer,
            secondary = seeded.secondary, onSecondary = seeded.onSecondary,
            secondaryContainer = seeded.secondaryContainer, onSecondaryContainer = seeded.onSecondaryContainer,
            tertiary = seeded.tertiary, onTertiary = seeded.onTertiary,
            tertiaryContainer = seeded.tertiaryContainer, onTertiaryContainer = seeded.onTertiaryContainer,
            error = seeded.error, onError = seeded.onError,
            errorContainer = seeded.errorContainer, onErrorContainer = seeded.onErrorContainer,
            background = neutrals.background, onBackground = neutrals.onBackground,
            surface = neutrals.surface, onSurface = neutrals.onSurface,
            surfaceVariant = neutrals.surfaceVariant, onSurfaceVariant = neutrals.onSurfaceVariant,
            surfaceTint = seeded.primary,
            inverseSurface = neutrals.inverseSurface, inverseOnSurface = neutrals.inverseOnSurface,
            inversePrimary = seeded.primary,
            outline = neutrals.outline, outlineVariant = neutrals.outlineVariant,
            scrim = Color(0xFF000000),
            surfaceBright = neutrals.surfaceBright, surfaceDim = neutrals.surfaceDim,
            surfaceContainer = neutrals.surfaceContainer,
            surfaceContainerHigh = neutrals.surfaceContainerHigh,
            surfaceContainerHighest = neutrals.surfaceContainerHighest,
            surfaceContainerLow = neutrals.surfaceContainerLow,
            surfaceContainerLowest = neutrals.surfaceContainerLowest,
        )
    }

    val extraColors = if (isDark) DarkExtraColorScheme else LightExtraColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = {
            CompositionLocalProvider(
                LocalExtraColors provides extraColors,
                LocalOrientation provides orientation,
                LocalStrings provides getStrings(),
                LocalPracticeSounds provides practiceSounds,
                LocalTextSelectionColors provides neutralTextSelectionColors()
            ) {
                content()
            }
        }
    )
}

@Composable
private fun neutralTextSelectionColors() = TextSelectionColors(
    handleColor = MaterialTheme.colorScheme.onSurface,
    backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
)

@Composable
fun ButtonDefaults.neutralButtonColors(): ButtonColors {
    return MaterialTheme.colorScheme.run {
        buttonColors(
            containerColor = surfaceVariant,
            contentColor = onSurfaceVariant
        )
    }
}

@Composable
fun ButtonDefaults.neutralTextButtonColors(): ButtonColors {
    return MaterialTheme.colorScheme.run {
        textButtonColors(
            contentColor = onSurface
        )
    }
}


@Composable
fun TextFieldDefaults.neutralColors(): TextFieldColors = MaterialTheme.colorScheme.run {
    val labelColor = onSurface.copy(alpha = 0.4f)
    colors(
        unfocusedIndicatorColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedLabelColor = labelColor,
        focusedLabelColor = labelColor,
        disabledLabelColor = labelColor,
        cursorColor = onSurface
    )
}

@Composable
fun ListItemDefaults.errorColors(): ListItemColors {
    return colors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        headlineColor = MaterialTheme.colorScheme.onErrorContainer,
        supportingColor = MaterialTheme.colorScheme.onErrorContainer,
        leadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
        trailingIconColor = MaterialTheme.colorScheme.onErrorContainer
    )
}

fun snapSizeTransform(): SizeTransform = SizeTransform() { _, _ -> snap() }

fun snapToBiggerSizeTransform(
    snapToSmallerContainerDelay: Int = AnimationConstants.DefaultDurationMillis
): SizeTransform = SizeTransform { initial, target ->
    if (target.width > initial.width || target.height > initial.height) snap()
    else snap(snapToSmallerContainerDelay)
}

fun <S> snapToBiggerContainerCrossfadeTransitionSpec(
    snapToSmallerContainerDelay: Int = AnimationConstants.DefaultDurationMillis
): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    fadeIn() togetherWith fadeOut() using snapToBiggerSizeTransform(snapToSmallerContainerDelay)
}
