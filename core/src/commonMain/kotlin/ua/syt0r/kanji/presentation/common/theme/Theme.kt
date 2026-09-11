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
import androidx.compose.ui.graphics.luminance
import ua.syt0r.kanji.core.theme_manager.LocalThemeManager
import ua.syt0r.kanji.presentation.common.resources.string.LocalStrings
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.common.sound.LocalPracticeSounds
import ua.syt0r.kanji.presentation.common.sound.rememberPracticeSoundPlayer
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation

// 品牌红（默认主色）。用户可在设置里输入色号覆盖主色（仅主色联动，其余配色固定）。
private val DefaultLightPrimary = Color(0xFFD32F2F)
private val DefaultDarkPrimary = Color(0xFFFFB4A9)

private fun onColorFor(primary: Color): Color =
    if (primary.luminance() > 0.5f) Color(0xFF000000) else Color(0xFFFFFFFF)

private fun lightScheme(primary: Color): ColorScheme = lightColorScheme(
    primary = primary, onPrimary = onColorFor(primary),
    primaryContainer = Color(0xFFFFDAD6), onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF7D5843), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0DAC9), onSecondaryContainer = Color(0xFF2A1507),
    tertiary = Color(0xFF7A5900), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF9E), onTertiaryContainer = Color(0xFF251900),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDD8), onSurfaceVariant = Color(0xFF53433F),
    surfaceTint = primary,
    inverseSurface = Color(0xFF362F2E), inverseOnSurface = Color(0xFFFBEEEC),
    inversePrimary = Color(0xFFFFB4A9),
    outline = Color(0xFF85736E), outlineVariant = Color(0xFFD8C2BC),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFBFF), surfaceDim = Color(0xFFE6DEDC),
    surfaceContainer = Color(0xFFF3EDEB), surfaceContainerHigh = Color(0xFFEDE4E1),
    surfaceContainerHighest = Color(0xFFE7DEDB), surfaceContainerLow = Color(0xFFFFFCFA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

private fun darkScheme(primary: Color): ColorScheme = darkColorScheme(
    primary = primary, onPrimary = onColorFor(primary),
    primaryContainer = Color(0xFF93000A), onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE5BFA8), onSecondary = Color(0xFF442A1A),
    secondaryContainer = Color(0xFF5D4030), onSecondaryContainer = Color(0xFFF0DAC9),
    tertiary = Color(0xFFF1BF49), onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4300), onTertiaryContainer = Color(0xFFFFDF9E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF201A19), onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF201A19), onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF53433F), onSurfaceVariant = Color(0xFFD8C2BC),
    surfaceTint = primary,
    inverseSurface = Color(0xFFEDE0DE), inverseOnSurface = Color(0xFF362F2E),
    inversePrimary = Color(0xFFD32F2F),
    outline = Color(0xFFA08C87), outlineVariant = Color(0xFF53433F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3B3433), surfaceDim = Color(0xFF201A19),
    surfaceContainer = Color(0xFF2D2524), surfaceContainerHigh = Color(0xFF373030),
    surfaceContainerHighest = Color(0xFF423A39), surfaceContainerLow = Color(0xFF251D1C),
    surfaceContainerLowest = Color(0xFF1C1413),
)

private fun amoledScheme(primary: Color): ColorScheme = darkColorScheme(
    primary = primary, onPrimary = onColorFor(primary),
    primaryContainer = Color(0xFF93000A), onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE5BFA8), onSecondary = Color(0xFF442A1A),
    secondaryContainer = Color(0xFF5D4030), onSecondaryContainer = Color(0xFFF0DAC9),
    tertiary = Color(0xFFF1BF49), onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4300), onTertiaryContainer = Color(0xFFFFDF9E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000), onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF000000), onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF2A2A2A), onSurfaceVariant = Color(0xFFD8C2BC),
    surfaceTint = primary,
    inverseSurface = Color(0xFFEDE0DE), inverseOnSurface = Color(0xFF362F2E),
    inversePrimary = Color(0xFFD32F2F),
    outline = Color(0xFFA08C87), outlineVariant = Color(0xFF2A2A2A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF121212), surfaceDim = Color(0xFF121212),
    surfaceContainer = Color(0xFF121212), surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF1F1F1F), surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainerLowest = Color(0xFF000000),
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

    // 用户自定义主色（设置里输入的色号）；未设置时用默认品牌色
    val customPrimary = LocalThemeManager.current.currentCustomSeedColor.value

    val colorScheme = when {
        useAmoledTheme -> amoledScheme(customPrimary ?: DefaultDarkPrimary)
        isDark -> darkScheme(customPrimary ?: DefaultDarkPrimary)
        else -> lightScheme(customPrimary ?: DefaultLightPrimary)
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
