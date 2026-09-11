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

// 从 seed 生成的 ColorScheme 构造最终的 material3 scheme：
// 背景/表面/容器全部随 seed 派生，on* 色由算法保证对比（深背景白字/浅背景黑字），避免白字白背景
private fun buildColorScheme(seeded: ColorScheme, dark: Boolean): ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = seeded.primary, onPrimary = seeded.onPrimary,
            primaryContainer = seeded.primaryContainer, onPrimaryContainer = seeded.onPrimaryContainer,
            secondary = seeded.secondary, onSecondary = seeded.onSecondary,
            secondaryContainer = seeded.secondaryContainer, onSecondaryContainer = seeded.onSecondaryContainer,
            tertiary = seeded.tertiary, onTertiary = seeded.onTertiary,
            tertiaryContainer = seeded.tertiaryContainer, onTertiaryContainer = seeded.onTertiaryContainer,
            error = seeded.error, onError = seeded.onError,
            errorContainer = seeded.errorContainer, onErrorContainer = seeded.onErrorContainer,
            background = seeded.background, onBackground = seeded.onBackground,
            surface = seeded.surface, onSurface = seeded.onSurface,
            surfaceVariant = seeded.surfaceVariant, onSurfaceVariant = seeded.onSurfaceVariant,
            surfaceTint = seeded.primary,
            inverseSurface = seeded.inverseSurface, inverseOnSurface = seeded.inverseOnSurface,
            inversePrimary = seeded.primary,
            outline = seeded.outline, outlineVariant = seeded.outlineVariant,
            scrim = Color(0xFF000000),
            surfaceBright = seeded.surface, surfaceDim = seeded.surface,
            surfaceContainer = seeded.surfaceVariant, surfaceContainerHigh = seeded.surfaceVariant,
            surfaceContainerHighest = seeded.surfaceVariant, surfaceContainerLow = seeded.surface,
            surfaceContainerLowest = seeded.background,
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
            background = seeded.background, onBackground = seeded.onBackground,
            surface = seeded.surface, onSurface = seeded.onSurface,
            surfaceVariant = seeded.surfaceVariant, onSurfaceVariant = seeded.onSurfaceVariant,
            surfaceTint = seeded.primary,
            inverseSurface = seeded.inverseSurface, inverseOnSurface = seeded.inverseOnSurface,
            inversePrimary = seeded.primary,
            outline = seeded.outline, outlineVariant = seeded.outlineVariant,
            scrim = Color(0xFF000000),
            surfaceBright = seeded.surface, surfaceDim = seeded.surface,
            surfaceContainer = seeded.surfaceVariant, surfaceContainerHigh = seeded.surfaceVariant,
            surfaceContainerHighest = seeded.surfaceVariant, surfaceContainerLow = seeded.surface,
            surfaceContainerLowest = seeded.background,
        )
    }
}

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
    orientation: Orientation = Orientation.Portrait,
    content: @Composable () -> Unit
) {
    val practiceSounds = rememberPracticeSoundPlayer()

    val isDark = useDarkTheme

    // 用户自定义主色（设置里输入的色号）；未设置时回退品牌色
    val seedColor = LocalThemeManager.current.currentCustomSeedColor.value ?: BrandSeedColor

    // 由 seed 生成完整 M3 配色（主色/背景/表面/容器 全部随 seed 联动，on* 自动对比）
    val seeded = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = false
    )

    val colorScheme = buildColorScheme(seeded, isDark)

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
