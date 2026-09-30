package ua.syt0r.kanji.presentation.screen.main.screen.cache_settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.tts.WordTtsCacheStats
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.use_case.RefreshVocabMeaningsUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.tts_cache.describeTtsCacheStats

/**
 * Holds the cache related entries: the synthesized audio cache, and the meaning refresh that decks
 * imported before the dictionary was translated need.
 *
 * Both live one level below the settings list, which only carries entries that open a screen of
 * their own — an entry either leads somewhere, or it is not an entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheSettingsScreen(
    state: MainNavigationState
) {

    val getTtsCacheStats = koinInject<GetTtsCacheStatsUseCase>()
    val refreshMeanings = koinInject<RefreshVocabMeaningsUseCase>()
    val coroutineScope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<WordTtsCacheStats?>(null) }
    var showRefreshDialog by remember { mutableStateOf(false) }
    var refreshResult by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        stats = getTtsCacheStats.stats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveString { settings.cacheSettingsTitle }) },
                navigationIcon = {
                    IconButton(onClick = { state.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Absent on platforms without a cache at all (see GetTtsCacheStatsUseCase).
            stats?.let { cacheStats ->
                ListItem(
                    headlineContent = { Text(resolveString { settings.ttsCacheTitle }) },
                    supportingContent = { Text(describeTtsCacheStats(cacheStats)) },
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .fillMaxWidth()
                        .clickable { state.navigate(MainDestination.TtsCache) }
                )
            }

            ListItem(
                headlineContent = { Text(resolveString { settings.refreshMeaningsTitle }) },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .fillMaxWidth()
                    .clickable { showRefreshDialog = true }
            )

        }

    }

    // Rewrites every card, so it asks first.
    if (showRefreshDialog) {
        MultiplatformDialog(
            onDismissRequest = { showRefreshDialog = false },
            title = { Text(resolveString { settings.refreshMeaningsTitle }) },
            content = { Text(resolveString { settings.refreshMeaningsMessage }) },
            buttons = {
                TextButton(onClick = { showRefreshDialog = false }) {
                    Text(resolveString { settings.pickerDialogCancel })
                }
                TextButton(
                    onClick = {
                        showRefreshDialog = false
                        coroutineScope.launch {
                            refreshResult = refreshMeanings.refresh()
                        }
                    }
                ) {
                    Text(resolveString { settings.refreshMeaningsConfirm })
                }
            }
        )
    }

    refreshResult?.let { count ->
        MultiplatformDialog(
            onDismissRequest = { refreshResult = null },
            title = { Text(resolveString { settings.refreshMeaningsTitle }) },
            content = { Text(resolveString { settings.refreshMeaningsDone(count) }) },
            buttons = {
                TextButton(onClick = { refreshResult = null }) {
                    Text(resolveString { settings.dialogOk })
                }
            }
        )
    }

}
