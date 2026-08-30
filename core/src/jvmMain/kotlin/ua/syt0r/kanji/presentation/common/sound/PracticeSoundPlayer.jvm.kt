package ua.syt0r.kanji.presentation.common.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ua.syt0r.kanji.Res
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

// 桌面/JVM：javax.sound 播 compose 资源里的 wav（原生支持）。保持 Clip 引用直到播放完，避免被 GC 打断。
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
            val bytes = runBlocking { Res.readBytes("files/sounds/$asset.wav") }
            val audioStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
            val clip = AudioSystem.getClip()
            clip.open(audioStream)
            synchronized(this) {
                clip.addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP) {
                        clip.close()
                        activeClips.remove(clip)
                    }
                }
                activeClips.add(clip)
            }
            clip.start()
        }
    }

    private val activeClips = mutableListOf<Clip>()

}

@Composable
actual fun rememberPracticeSoundPlayer(): PracticeSoundPlayer = remember { JvmPracticeSoundPlayer() }
