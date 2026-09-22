package ua.syt0r.kanji.presentation.common.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ua.syt0r.kanji.core.R
import ua.syt0r.kanji.core.logger.Logger

class AndroidPracticeSoundPlayer(context: Context) : PracticeSoundPlayer {

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<PracticeSoundEffect, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()

    // SoundPool.load 是异步的，加载完成前 play 会被静默忽略；
    // 这里记住“来不及播”的那一次，等加载完成后再补播。
    private var pendingEffect: PracticeSoundEffect? = null

    init {
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // 用 GAME(走媒体音量) 而不是 ASSISTANCE_SONIFICATION(走系统提示音)，
                    // 后者在部分设备/静音模式下会被系统静默掉，导致完全没有声音
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds += sampleId
                pendingEffect
                    ?.takeIf { soundIds[it] == sampleId }
                    ?.let { effect ->
                        pendingEffect = null
                        play(effect)
                    }
            } else {
                Logger.e("Failed to load sound sample[$sampleId], status[$status]")
            }
        }

        soundIds[PracticeSoundEffect.Correct] = soundPool.load(context, R.raw.whenright, 1)
        soundIds[PracticeSoundEffect.Incorrect] = soundPool.load(context, R.raw.whenwrong, 1)
        soundIds[PracticeSoundEffect.Click] = soundPool.load(context, R.raw.whenclick, 1)
        soundIds[PracticeSoundEffect.Finish] = soundPool.load(context, R.raw.whenfinish, 1)
    }

    override fun play(effect: PracticeSoundEffect) {
        val soundId = soundIds[effect]
        if (soundId == null || soundId <= 0) {
            Logger.e("Sound[$effect] was not loaded, sampleId[$soundId]")
            return
        }

        if (soundId in loadedSoundIds) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            pendingEffect = effect
            Logger.d("Sound[$effect] is still loading, will play when ready")
        }
    }

}

@Composable
actual fun rememberPracticeSoundPlayer(): PracticeSoundPlayer {
    val context = LocalContext.current.applicationContext
    return remember { AndroidPracticeSoundPlayer(context) }
}
