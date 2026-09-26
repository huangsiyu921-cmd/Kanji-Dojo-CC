package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import ua.syt0r.kanji.core.tts.WordTtsCache
import ua.syt0r.kanji.core.tts.WordTtsCacheStats
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.tts_cache.describeTtsCacheStats

/**
 * Entry point to the TTS cache screen: shows how much synthesized audio is cached, and opens the
 * manager where it can be listened to, extended or cleared.
 */
class TtsCacheSettingItem(
    private val cache: WordTtsCache
) : SettingsScreenContract.ListItem {

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {

        var stats by remember { mutableStateOf<WordTtsCacheStats?>(null) }

        LaunchedEffect(Unit) {
            stats = cache.stats()
        }

        ListItem(
            headlineContent = { Text("TTS 缓存") },
            supportingContent = { Text(stats?.let(::describeTtsCacheStats) ?: "正在统计…") },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .fillMaxWidth()
                .clickable { mainNavigationState.navigate(MainDestination.TtsCache) }
        )

    }

}
