package ua.syt0r.kanji.presentation.common.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

enum class PracticeSoundEffect {
    Click,
    Correct,
    Incorrect,
    Finish
}

fun interface PracticeSoundPlayer {
    fun play(effect: PracticeSoundEffect)
}

// 占位实现：先接通逻辑，真实平台播放（Android SoundPool / JVM javax.sound）后续接入音频资源后替换
object NoOpPracticeSoundPlayer : PracticeSoundPlayer {
    override fun play(effect: PracticeSoundEffect) = Unit
}

val LocalPracticeSounds = compositionLocalOf<PracticeSoundPlayer> { NoOpPracticeSoundPlayer }

/** 平台音效播放器：Android 用 SoundPool 播 raw/whenright（答对）；其余平台暂 no-op */
@Composable
expect fun rememberPracticeSoundPlayer(): PracticeSoundPlayer
