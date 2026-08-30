package ua.syt0r.kanji.core.theme_manager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme


open class ThemeManager(
    private val appPreferences: PreferencesContract.AppPreferences,
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
) {

    protected val coroutineScope = CoroutineScope(dispatcher)

    private val _currentTheme = mutableStateOf(value = runBlocking { appPreferences.theme.get() })
    val currentTheme: State<PreferencesTheme> = _currentTheme

    private val _currentCustomSeedColor = mutableStateOf<Color?>(
        runBlocking { parseHexColor(appPreferences.customSeedColor.get()) }
    )
    val currentCustomSeedColor: State<Color?> = _currentCustomSeedColor

    init {
        appPreferences.theme
            .onModified
            .onEach { invalidate() }
            .launchIn(coroutineScope)

        appPreferences.customSeedColor
            .onModified
            .onEach { _currentCustomSeedColor.value = runBlocking { parseHexColor(appPreferences.customSeedColor.get()) } }
            .launchIn(coroutineScope)
    }

    fun changeTheme(theme: PreferencesTheme) {
        coroutineScope.launch { appPreferences.theme.set(theme) }
    }

    fun changeCustomSeedColor(hex: String) {
        coroutineScope.launch { appPreferences.customSeedColor.set(hex) }
    }

    fun invalidate() {
        Logger.logMethod()
        coroutineScope.launch {
            val currentTheme = appPreferences.theme.get()
            _currentTheme.value = currentTheme
            platformInvalidate(currentTheme)
        }
    }

    open fun platformInvalidate(theme: PreferencesTheme) {}

    val isDarkTheme: Boolean
        @Composable
        get() = when (currentTheme.value) {
            PreferencesTheme.System -> isSystemInDarkTheme()
            PreferencesTheme.Light -> false
            PreferencesTheme.Dark, PreferencesTheme.Amoled -> true
        }

    val isAmoledTheme: Boolean
        get() = currentTheme.value == PreferencesTheme.Amoled

    private fun parseHexColor(hex: String): Color? {
        val cleaned = hex.trim().removePrefix("#")
        return when {
            cleaned.length == 6 -> cleaned.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
            cleaned.length == 8 -> cleaned.toLongOrNull(16)?.let { Color(it) }
            else -> null
        }
    }

}

val LocalThemeManager = compositionLocalOf<ThemeManager> {
    throw IllegalStateException("ThemeManager is not initialized")
}