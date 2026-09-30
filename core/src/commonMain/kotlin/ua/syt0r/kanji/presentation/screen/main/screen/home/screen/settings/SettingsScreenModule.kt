package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ua.syt0r.kanji.core.tts.WordTtsCache
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.cache_settings.GetTtsCacheStatsUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items.DailyResetTimeSettingItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items.DefaultHomeTabSettingItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items.ThemeSettingItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.use_case.RefreshVocabMeaningsUseCase
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
        listOf(
            ThemeSettingItem(themeManager = get()),
            DefaultHomeTabSettingItem(appPreferences = get()),
            DailyResetTimeSettingItem(appPreferences = get())
        )
    }

    factory(settingItemsQualifier) { listOf<SettingsScreenContract.ListItem>() }

    // Feeds the "import a deck" action on the TTS cache screen.
    factory {
        GetVocabPreCacheDecksUseCase(
            vocabPracticeRepository = get()
        )
    }

    // Backs the "unify card meanings" entry, which clears the meaning snapshots older imports stored
    // on the cards so the translated dictionary meaning is shown again.
    factory {
        RefreshVocabMeaningsUseCase(
            vocabPracticeRepository = get()
        )
    }

    // The TTS cache entry lives one level down (Settings -> Cache settings). Platforms without a
    // cache at all (currently iOS) get null here and hide the row.
    factory {
        GetTtsCacheStatsUseCase(
            cache = getOrNull<WordTtsCache>()
        )
    }

}