package ua.syt0r.kanji.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class AndroidWordTtsManager(context: Context) : WordTtsManager {

    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = textToSpeech
                tts?.language = Locale.JAPAN
                // 优先选本地(不联网、更自然)的日语 voice，避免默认音色生硬
                runCatching {
                    val bestJapaneseVoice = tts?.voices
                        ?.filter { it.locale.language == "ja" }
                        ?.sortedWith(compareByDescending { it.isNetworkConnectionRequired.not() })
                        ?.firstOrNull { it.isNetworkConnectionRequired.not() }
                    bestJapaneseVoice?.let { tts.voice = it }
                }
            }
        }
    }

    override suspend fun isAvailable(): Boolean = textToSpeech != null

    override val unavailableMessage: String
        get() = "No Japanese TTS voice available on this device. Install a Japanese voice in " +
            "Device Settings > Accessibility > Text-to-speech, then restart Kanji Dojo."

    override suspend fun speak(word: String) {
        val tts = textToSpeech ?: return
        val utteranceId = UUID.randomUUID().toString()

        suspendCancellableCoroutine { continuation ->
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                @Deprecated("Deprecated in TextToSpeech")
                override fun onError(utteranceId: String?) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            })
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

}
