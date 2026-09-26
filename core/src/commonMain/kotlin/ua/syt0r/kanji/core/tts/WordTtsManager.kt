package ua.syt0r.kanji.core.tts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Speaks a whole Japanese word/reading in one go using a real speech synthesis engine on
 * platforms that have one. Kept separate from [KanaTtsManager], which plays back pre-recorded
 * single-mora clips for the kana practice quiz and cannot pronounce a full word naturally.
 */
interface WordTtsManager {

    suspend fun speak(word: String)

    /**
     * Prepares the audio for [word] without playing it, so that a later [speak] is instant.
     *
     * @return whether something was cached. Platforms without a cache keep the default.
     */
    suspend fun preCache(word: String): Boolean = false

    /**
     * True while an utterance is being synthesized — **not** while it is playing.
     *
     * Screens use this to show a spinner only for the part the user actually waits for; keeping it
     * up while the audio plays would be misleading. Platforms that never synthesize leave it false.
     */
    val isPreparing: StateFlow<Boolean> get() = NeverPreparing

    /**
     * Whether a Japanese voice is currently available. On Android/iOS this is essentially always
     * true; on Desktop/JVM it reflects whether the OS has a Japanese voice installed.
     */
    suspend fun isAvailable(): Boolean

    /** User-facing explanation shown when [isAvailable] is false. */
    val unavailableMessage: String

    companion object {

        private val NeverPreparing: StateFlow<Boolean> = MutableStateFlow(false)

    }

}
