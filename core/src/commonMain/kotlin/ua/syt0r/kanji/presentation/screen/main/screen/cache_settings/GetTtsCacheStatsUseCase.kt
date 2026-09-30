package ua.syt0r.kanji.presentation.screen.main.screen.cache_settings

import ua.syt0r.kanji.core.tts.WordTtsCache
import ua.syt0r.kanji.core.tts.WordTtsCacheStats

/**
 * Reports how much synthesized audio is cached.
 *
 * Returns null on platforms that have no cache at all (currently iOS, whose TTS is not hooked up to
 * VOICEVOX yet) — the entry hides itself there, which is why the cache itself is optional rather
 * than injected directly.
 */
class GetTtsCacheStatsUseCase(
    private val cache: WordTtsCache?
) {

    suspend fun stats(): WordTtsCacheStats? = cache?.stats()

}
