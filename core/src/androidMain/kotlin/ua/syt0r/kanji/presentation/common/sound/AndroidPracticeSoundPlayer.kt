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
    private val incorrectSoundId: Int
    private val clickSoundId: Int
    private val finishSoundId: Int

    init {
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        correctSoundId = soundPool.load(context, R.raw.whenright, 1)
        incorrectSoundId = soundPool.load(context, R.raw.whenwrong, 1)
        clickSoundId = soundPool.load(context, R.raw.whenclick, 1)
        finishSoundId = soundPool.load(context, R.raw.whenfinish, 1)
    }

    override fun play(effect: PracticeSoundEffect) {
        val id = when (effect) {
            PracticeSoundEffect.Correct -> correctSoundId
            PracticeSoundEffect.Incorrect -> incorrectSoundId
            PracticeSoundEffect.Click -> clickSoundId
            PracticeSoundEffect.Finish -> finishSoundId
        }
        if (id > 0) soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

}

@Composable
actual fun rememberPracticeSoundPlayer(): PracticeSoundPlayer {
    val context = LocalContext.current.applicationContext
    return remember { AndroidPracticeSoundPlayer(context) }
}
