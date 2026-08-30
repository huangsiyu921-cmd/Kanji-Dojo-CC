package ua.syt0r.kanji.presentation.common.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ua.syt0r.kanji.core.R

class AndroidPracticeSoundPlayer(context: Context) : PracticeSoundPlayer {

    private val soundPool: SoundPool
    private val correctSoundId: Int

    init {
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        correctSoundId = soundPool.load(context, R.raw.whenright, 1)
    }

    override fun play(effect: PracticeSoundEffect) {
        if (effect == PracticeSoundEffect.Correct) {
            soundPool.play(correctSoundId, 1f, 1f, 1, 0, 1f)
        }
        // Click / Incorrect / Finish 暂无音频资源，暂不发声
    }

}

@Composable
actual fun rememberPracticeSoundPlayer(): PracticeSoundPlayer {
    val context = LocalContext.current.applicationContext
    return remember { AndroidPracticeSoundPlayer(context) }
}
