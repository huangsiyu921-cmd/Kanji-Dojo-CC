package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.tts.WordTtsCache
import ua.syt0r.kanji.core.tts.WordTtsCacheStats
import ua.syt0r.kanji.core.tts.WordTtsManager
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract

/**
 * Shows how much synthesized audio is cached, allows pre-caching a word on demand, and clearing
 * everything.
 *
 * Synthesis costs roughly 0.3–1.5s per word, so someone who wants to study without that wait can
 * pre-cache the words ahead of time; afterwards playing them back is instant.
 */
class TtsCacheSettingItem(
    private val cache: WordTtsCache,
    private val wordTtsManager: WordTtsManager
) : SettingsScreenContract.ListItem {

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {

        var showDialog by rememberSaveable { mutableStateOf(false) }
        var stats by remember { mutableStateOf<WordTtsCacheStats?>(null) }

        LaunchedEffect(showDialog) {
            // Also refreshes right after every action done inside the dialog.
            stats = cache.stats()
        }

        Column {
            ListItem(
                headlineContent = { Text("TTS 缓存") },
                supportingContent = { Text(stats?.let(::describeStats) ?: "计算中…") },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .fillMaxWidth()
                    .clickable { showDialog = true }
            )

            if (showDialog) {
                TtsCacheDialog(
                    stats = stats,
                    onPreCache = { word -> wordTtsManager.preCache(word) },
                    onClear = { cache.clear() },
                    onRefresh = { stats = cache.stats() },
                    onDismissRequest = { showDialog = false }
                )
            }
        }

    }

}

@Composable
private fun TtsCacheDialog(
    stats: WordTtsCacheStats?,
    onPreCache: suspend (String) -> Boolean,
    onClear: suspend () -> Unit,
    onRefresh: suspend () -> Unit,
    onDismissRequest: () -> Unit
) {

    val coroutineScope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("TTS 缓存") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(stats?.let(::describeStats) ?: "计算中…")

                Text(
                    text = "提前把词语的语音合成好，之后朗读就是即时的（一次合成约 0.3~1.5 秒）。" +
                        "填读音（假名）效果最好。",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("要提前缓存的词语") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )

                message?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }

            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && input.isNotBlank(),
                onClick = {
                    val word = input.trim()
                    coroutineScope.launch {
                        busy = true
                        val cached = onPreCache(word)
                        message = if (cached) "已缓存「$word」" else "「$word」缓存失败"
                        input = ""
                        busy = false
                        onRefresh()
                    }
                }
            ) {
                Text(if (busy) "缓存中…" else "缓存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    enabled = !busy && stats?.isEmpty == false,
                    onClick = {
                        coroutineScope.launch {
                            busy = true
                            onClear()
                            message = "已清空缓存"
                            busy = false
                            onRefresh()
                        }
                    }
                ) {
                    Text("清空")
                }
                TextButton(
                    enabled = !busy,
                    onClick = onDismissRequest
                ) {
                    Text("关闭")
                }
            }
        }
    )

}

private fun describeStats(stats: WordTtsCacheStats): String =
    if (stats.isEmpty) {
        "暂无缓存"
    } else {
        "${stats.entries} 条 · ${formatCacheSize(stats.sizeBytes)}"
    }

private fun formatCacheSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
