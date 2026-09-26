package ua.syt0r.kanji.presentation.dialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens

@Composable
fun VersionChangeDialog(
    onDismissRequest: () -> Unit
) {

    MultiplatformDialog(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(
                    start = Dimens.ContentPadding,
                    top = Dimens.ContentPadding,
                    end = Dimens.ContentPadding,
                    bottom = Dimens.ContentPadding / 2
                )
                .heightIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMid)
        ) {

            Text(
                resolveString { about.versionChangesTitle },
                style = MaterialTheme.typography.titleLarge
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {

                version("0.6", LocalDate(2026, 9, 27)) {
                    append(
                        """
                        - 增加了内置TTS的缓存功能，支持提前预热
                        - 设置页新增「TTS 缓存」页面
                        - 缓存支持导出/导入 zip
                        - 现在朗读按钮在合成期间显示加载圈
                        """.trimIndent()
                    )
                }
                version("0.5", LocalDate(2026, 9, 26)) {
                    append(
                        """
                        - 更改了TTS语音逻辑
                        - 修复了安卓端TTS
                        - 优化了学习时的TTS逻辑
                        """.trimIndent()
                    )
                }
                version("0.4", LocalDate(2026, 9, 26)) {
                    append(
                        """
                        - 添加并替换TTS语音为内置TTS
                        - 添加了部分准备为多平台发布版的兼容代码
                        - 删除了旧更新提示弹窗
                        """.trimIndent()
                    )
                }
                version("0.3", LocalDate(2026, 9, 23)) {
                    append(
                        """
                        - 翻译了绝大部分的.sql
                        """.trimIndent()
                    )
                }
                version("0.2", LocalDate(2026, 9, 22)) {
                    append(
                        """
                        - 添加了部分.sql汉化
                        - 修复部分bug
                        """.trimIndent()
                    )
                }
                version("0.1", LocalDate(2026, 9, 12)) {
                    append(
                        """
                        - 添加了UI汉化
                        - 移除了原作者的链接
                        - 删除了所有联网请求
                        """.trimIndent()
                    )
                }
            }

            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(resolveString { about.versionChangesButton })
            }

        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.version(
    versionNumber: String,
    releaseDate: LocalDate,
    changes: AnnotatedString.Builder.() -> Unit
) {
    stickyHeader {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    vertical = Dimens.SpacingMid
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleStyle = MaterialTheme.typography.labelLarge
            Text(
                text = "Version: $versionNumber",
                modifier = Modifier.weight(1f),
                style = titleStyle
            )
            Text(
                text = releaseDate.toString(),
                style = titleStyle
            )
        }
    }
    item {
        Text(
            text = AnnotatedString.Builder().apply(changes).toAnnotatedString(),
            modifier = Modifier.padding(vertical = Dimens.SpacingSmall),
            style = MaterialTheme.typography.bodySmall
        )
    }
}