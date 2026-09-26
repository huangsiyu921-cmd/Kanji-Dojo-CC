package ua.syt0r.kanji.presentation.screen.main.screen.tts_cache

import ua.syt0r.kanji.core.VocabCardResolver
import ua.syt0r.kanji.core.user_data.database.VocabPracticeRepository

/** A deck the user can pre-cache in one go, with the readings it would cover. */
data class VocabPreCacheDeck(
    val id: Long,
    val title: String,
    val readings: List<String>
) {

    val isEmpty: Boolean get() = readings.isEmpty()

}

/**
 * Collects the kana readings of the user's vocabulary decks so their audio can be synthesized
 * ahead of time.
 *
 * The readings come from [VocabCardResolver]: a saved card stores the reading the user picked when
 * adding the word, which is exactly what should be spoken (passing the bare kanji to the engine
 * would make it guess, e.g. 今日 → こんいち).
 */
class GetVocabPreCacheDecksUseCase(
    private val vocabPracticeRepository: VocabPracticeRepository,
    private val vocabCardResolver: VocabCardResolver
) {

    suspend fun getDecks(): List<VocabPreCacheDeck> {
        return vocabPracticeRepository.getDecks().map { deck ->
            val readings = vocabPracticeRepository.getCardIdList(deck.id)
                .mapNotNull { cardId ->
                    runCatching { vocabCardResolver.resolveUserCard(cardId).kanaReading }
                        .getOrNull()
                }
                .filter { it.isNotBlank() }
                .distinct()

            VocabPreCacheDeck(
                id = deck.id,
                title = deck.title,
                readings = readings
            )
        }
    }

}
