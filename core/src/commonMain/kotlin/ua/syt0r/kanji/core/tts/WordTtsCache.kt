package ua.syt0r.kanji.core.tts

/** What the settings screen shows about the cached audio. */
data class WordTtsCacheStats(
    val entries: Int,
    val sizeBytes: Long
) {

    val isEmpty: Boolean get() = entries == 0

}

/** One cached utterance, as listed on the TTS cache screen. */
data class WordTtsCacheEntry(
    val word: String,
    val sizeBytes: Long
)

/**
 * Keeps synthesized audio around.
 *
 * Synthesis costs roughly 0.3–1.5s per word on device while replaying a cached wav is instant, and
 * the same vocabulary shows up over and over in practice.
 *
 * The directory backing an implementation is expected to already identify the synthesis settings
 * that produced the audio (see the DI modules), so changing a setting starts a fresh cache instead
 * of replaying audio that no longer matches. That is what makes a plain word a usable key here.
 *
 * Implementations must be safe to call from several coroutines and must never let a cache problem
 * escape — a broken cache may not break pronunciation.
 */
interface WordTtsCache {

    suspend fun get(word: String): ByteArray?

    suspend fun put(word: String, wav: ByteArray)

    suspend fun stats(): WordTtsCacheStats

    /** Newest first. */
    suspend fun entries(): List<WordTtsCacheEntry>

    suspend fun remove(word: String)

    suspend fun clear()

}
