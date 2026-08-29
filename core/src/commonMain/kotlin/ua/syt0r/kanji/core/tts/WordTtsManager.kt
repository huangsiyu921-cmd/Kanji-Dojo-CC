package ua.syt0r.kanji.core.tts

/**
 * Speaks a whole Japanese word/reading in one go using a real speech synthesis engine on
 * platforms that have one. Kept separate from [KanaTtsManager], which plays back pre-recorded
 * single-mora clips for the kana practice quiz and cannot pronounce a full word naturally.
 */
interface WordTtsManager {

    suspend fun speak(word: String)

    /**
     * Whether a Japanese voice is currently available. On Android/iOS this is essentially always
     * true; on Desktop/JVM it reflects whether the OS has a Japanese voice installed.
     */
    suspend fun isAvailable(): Boolean

    /** User-facing explanation shown when [isAvailable] is false. */
    val unavailableMessage: String

}
