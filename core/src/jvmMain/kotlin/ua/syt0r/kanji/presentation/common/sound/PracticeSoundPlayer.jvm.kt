package ua.syt0r.kanji.presentation.common.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ua.syt0r.kanji.Res
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

// 桌面/JVM：异步播放 wav。play() 立即返回，后台协程读文件+播放，不阻塞 UI，多个音效可并发。
class JvmPracticeSoundPlayer : PracticeSoundPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val effectToAsset = mapOf(
        PracticeSoundEffect.Correct to "whenright",
        PracticeSoundEffect.Incorrect to "whenwrong",
        PracticeSoundEffect.Click to "whenclick",
        PracticeSoundEffect.Finish to "whenfinish"
    )

    private val activeClips = mutableListOf<Clip>()

    @OptIn(ExperimentalResourceApi::class)
    override fun play(effect: PracticeSoundEffect) {
        val asset = effectToAsset[effect] ?: return
        scope.launch {
            runCatching {
                val bytes = Res.readBytes("files/sounds/$asset.wav")
                val audioStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
                val clip = AudioSystem.getClip()
                clip.open(audioStream)
                synchronized(this@JvmPracticeSoundPlayer) {
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
    }

}

@Composable
actual fun rememberPracticeSoundPlayer(): PracticeSoundPlayer = remember { JvmPracticeSoundPlayer() }
