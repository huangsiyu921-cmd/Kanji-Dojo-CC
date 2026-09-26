package ua.syt0r.kanji.core.tts

/**
 * Packs the whole TTS cache into one archive file (and restores it), so the audio can be moved
 * between devices or survive a reinstall.
 *
 * Implementations are platform specific because the archive needs a location the user can reach;
 * they work through [WordTtsCache] alone, so no direct access to the cache directory is needed.
 */
interface WordTtsCacheArchive {

    /** Where the archive is written to and read from; shown in the UI. */
    val location: String

    /** @return how many entries were packed, or `-1` on failure. */
    suspend fun export(cache: WordTtsCache): Int

    /** @return how many entries were restored, or `-1` on failure. */
    suspend fun import(cache: WordTtsCache): Int

}
