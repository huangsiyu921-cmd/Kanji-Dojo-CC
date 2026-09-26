package ua.syt0r.kanji.presentation.screen.main.screen.tts_cache

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.tts.WordTtsCache
import ua.syt0r.kanji.core.tts.WordTtsCacheEntry
import ua.syt0r.kanji.core.tts.WordTtsCacheStats
import ua.syt0r.kanji.core.tts.WordTtsManager
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

/**
 * Manages the synthesized audio: every cached word with a play button and a delete button, an entry
 * to pre-cache a word on demand, and a way to wipe everything.
 *
 * Pre-caching is what removes the ~0.3–1.5s synthesis wait from studying; the cache screen is where
 * you can see and curate it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsCacheScreen(
    state: MainNavigationState
) {

    val cache = koinInject<WordTtsCache>()
    val wordTtsManager = koinInject<WordTtsManager>()
    val coroutineScope = rememberCoroutineScope()

    var entries by remember { mutableStateOf<List<WordTtsCacheEntry>>(emptyList()) }
    var stats by remember { mutableStateOf<WordTtsCacheStats?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Only the synthesis wait gets a spinner; cached entries start playing right away.
    val preparing by wordTtsManager.isPreparing.collectAsState()

    suspend fun refresh() {
        entries = cache.entries()
        stats = cache.stats()
    }

    LaunchedEffect(Unit) { refresh() }

    fun preCache(words: List<String>) {
        val todo = words.map { it.trim() }.filter { it.isNotBlank() }
        if (todo.isEmpty() || busy) return

        coroutineScope.launch {
            busy = true
            var done = 0
            todo.forEachIndexed { index, word ->
                if (wordTtsManager.preCache(word)) done++
                message = "缓存中… ${index + 1}/${todo.size}"
            }
            message = "已缓存 $done/${todo.size}"
            busy = false
            refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TTS 缓存") },
                navigationIcon = {
                    IconButton(onClick = { state.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        enabled = !busy && stats?.isEmpty == false,
                        onClick = { showClearDialog = true }
                    ) {
                        Text("清空")
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = stats?.let(::describeTtsCacheStats) ?: "正在统计…",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("词语（填假名读音效果最好）") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = !busy && input.isNotBlank(),
                        onClick = {
                            val word = input
                            input = ""
                            preCache(listOf(word))
                        }
                    ) {
                        Text("缓存")
                    }
                }

                message?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }

            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有缓存。缓存过的词再朗读就是瞬时的。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = entries, key = { it.word }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.word) },
                            supportingContent = { Text(formatTtsCacheSize(entry.sizeBytes)) },
                            trailingContent = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                wordTtsManager.speak(entry.word)
                                            }
                                        }
                                    ) {
                                        if (preparing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "试听"
                                            )
                                        }
                                    }

                                    IconButton(
                                        enabled = !busy,
                                        onClick = {
                                            coroutineScope.launch {
                                                cache.remove(entry.word)
                                                refresh()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

        }

    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空 TTS 缓存") },
            text = { Text("将删除全部 ${stats?.entries ?: 0} 条已缓存的语音，之后朗读需要重新合成。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        coroutineScope.launch {
                            cache.clear()
                            refresh()
                        }
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

}

internal fun describeTtsCacheStats(stats: WordTtsCacheStats): String =
    if (stats.isEmpty) {
        "暂无缓存"
    } else {
        "${stats.entries} 条 · ${formatTtsCacheSize(stats.sizeBytes)}"
    }

internal fun formatTtsCacheSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
