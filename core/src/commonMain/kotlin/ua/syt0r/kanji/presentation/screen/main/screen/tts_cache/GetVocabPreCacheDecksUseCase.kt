package ua.syt0r.kanji.presentation.screen.main.screen.tts_cache

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
 * The readings are read straight off the saved cards: a card stores the reading the user picked
 * when adding the word, which is exactly what should be spoken (passing the bare kanji to the
 * engine would make it guess, e.g. 今日 → こんいち).
 *
 * All cards are fetched with a single query and grouped by deck. Resolving card ids one by one
 * through `VocabCardResolver` also looked up the dictionary for every word — that is what made the
 * dialog sit on "正在读取牌组…" for minutes on a deck of a few hundred entries.
 */
class GetVocabPreCacheDecksUseCase(
    private val vocabPracticeRepository: VocabPracticeRepository
) {

    suspend fun getDecks(): List<VocabPreCacheDeck> {
        val readingsByDeck = vocabPracticeRepository.getAllCards().groupBy { it.deckId }

        return vocabPracticeRepository.getDecks().map { deck ->
            val readings = readingsByDeck[deck.id]
                .orEmpty()
                .map { it.data.kanaReading }
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
