package ua.syt0r.kanji.presentation.common.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ua.syt0r.kanji.Res
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem

// 桌面/JVM：用 javax.sound + mp3spi 播放 compose 资源里的 mp3 音效
class JvmPracticeSoundPlayer : PracticeSoundPlayer {

    private val effectToAsset = mapOf(
        PracticeSoundEffect.Correct to "whenright",
        PracticeSoundEffect.Incorrect to "whenwrong",
        PracticeSoundEffect.Click to "whenclick",
        PracticeSoundEffect.Finish to "whenfinish"
    )

    @OptIn(ExperimentalResourceApi::class)
    override fun play(effect: PracticeSoundEffect) {
        val asset = effectToAsset[effect] ?: return
        runCatching {
            val bytes = runBlocking { Res.readBytes("files/sounds/$asset.mp3") }
            val audioStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
            val clip = AudioSystem.getClip()
            clip.open(audioStream)
            clip.start()
        }
    }

}

@Composable
actual fun rememberPracticeSoundPlayer(): PracticeSoundPlayer = remember { JvmPracticeSoundPlayer() }
