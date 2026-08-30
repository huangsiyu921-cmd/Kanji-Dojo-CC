package ua.syt0r.kanji.presentation.common.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// 桌面/JVM 暂无音效资源，暂为 no-op（后续可用 javax.sound + wav 实现）
@Composable
actual fun rememberPracticeSoundPlayer(): PracticeSoundPlayer = remember { NoOpPracticeSoundPlayer }
