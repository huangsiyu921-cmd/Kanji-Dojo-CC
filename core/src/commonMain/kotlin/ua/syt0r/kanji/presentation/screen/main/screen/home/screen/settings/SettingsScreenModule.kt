package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ua.syt0r.kanji.core.tts.WordTtsCache
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items.DailyResetTimeSettingItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items.DefaultHomeTabSettingItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items.ThemeSettingItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items.TtsCacheSettingItem
import ua.syt0r.kanji.presentation.screen.main.screen.tts_cache.GetVocabPreCacheDecksUseCase

val defaultSettingItemsQualifier = qualifier("default_setting_items")
val settingItemsQualifier = qualifier("setting_items")

val settingsScreenModule = module {

    multiplatformViewModel<SettingsScreenContract.ViewModel> {
        SettingsScreenViewModel(
            coroutineScope = it.component1(),
            defaultSettingItems = get(defaultSettingItemsQualifier),
            customSettingItems = get(settingItemsQualifier)
        )
    }

    factory(defaultSettingItemsQualifier) {
        listOfNotNull(
            ThemeSettingItem(themeManager = get()),
            DefaultHomeTabSettingItem(appPreferences = get()),
            DailyResetTimeSettingItem(appPreferences = get()),
            // Platforms without a cache (currently iOS, whose TTS is not hooked up to VOICEVOX yet)
            // simply do not show the entry.
            getOrNull<WordTtsCache>()?.let { cache ->
                TtsCacheSettingItem(cache = cache)
            }
        )
    }

    factory(settingItemsQualifier) { listOf<SettingsScreenContract.ListItem>() }

    // Feeds the "import a deck" action on the TTS cache screen.
    factory {
        GetVocabPreCacheDecksUseCase(
            vocabPracticeRepository = get(),
            vocabCardResolver = get()
        )
    }

}