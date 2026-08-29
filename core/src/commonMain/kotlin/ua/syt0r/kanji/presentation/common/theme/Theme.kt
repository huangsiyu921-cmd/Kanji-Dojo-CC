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
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import ua.syt0r.kanji.presentation.common.resources.string.LocalStrings
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation

class ExtraColorsScheme(
    val link: Color,
    val success: Color,
    val pending: Color,
    val due: Color,
    val new: Color
)

val LightExtraColorScheme = ExtraColorsScheme(
    link = lightThemeLinkColor,
    success = lightThemeSuccessColor,
    pending = lightThemePendingColor,
    due = lightThemeDueColor,
    new = lightThemeNewColor
)

val DarkExtraColorScheme = ExtraColorsScheme(
    link = darkThemeLinkColor,
    success = darkThemeSuccessColor,
    pending = darkThemePendingColor,
    due = darkThemeDueColor,
    new = darkThemeNewColor
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
    val isDark = useDarkTheme || useAmoledTheme

    // 由品牌 seed 色动态生成明暗 Material 3 配色（跨平台一致；isAmoled 由库处理纯黑）
    val colorScheme = rememberDynamicColorScheme(
        seedColor = BrandSeedColor,
        isDark = isDark,
        isAmoled = useAmoledTheme
    )

    val extraColors = if (isDark) DarkExtraColorScheme else LightExtraColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = {
            CompositionLocalProvider(
                LocalExtraColors provides extraColors,
                LocalOrientation provides orientation,
                LocalStrings provides getStrings(),
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
