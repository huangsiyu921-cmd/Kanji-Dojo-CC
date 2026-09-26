package ua.syt0r.kanji.core.tts

/** What the settings screen shows about the cached audio. */
data class WordTtsCacheStats(
    val entries: Int,
    val sizeBytes: Long
) {

    val isEmpty: Boolean get() = entries == 0

}

/**
 * Keeps synthesized audio around.
 *
 * Synthesis costs roughly 0.3–1.5s per word on device while replaying a cached wav is free, and
 * the same vocabulary shows up over and over in practice. Entries are keyed by everything that
 * changes the produced audio, see the key builder in the platform `WordTtsManager`s.
 *
 * Implementations are expected to be safe to call from several coroutines and to never throw on a
 * cache miss/hit problem — a broken cache must not break pronunciation.
 */
interface WordTtsCache {

    suspend fun get(key: String): ByteArray?

    suspend fun put(key: String, wav: ByteArray)

    suspend fun stats(): WordTtsCacheStats

    suspend fun clear()

}
