package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.use_case

import ua.syt0r.kanji.core.user_data.database.VocabPracticeRepository

/**
 * Drops the meaning that older imports stored inside the card itself, so the meaning that is shown
 * comes from the dictionary again.
 *
 * A card keeps a snapshot of the meaning it was created with (`VocabCardData.meaning`). For cards
 * created before the dictionary was translated that snapshot is English and it never follows the
 * dictionary — the resolver prefers it over the dictionary value. Clearing it makes the resolver
 * fall back to the dictionary, which is what a freshly imported card does.
 *
 * Only cards that carry a dictionary id are touched: without one there would be nothing to fall back
 * to. Study progress is keyed by card id, so it is not affected.
 */
class RefreshVocabMeaningsUseCase(
    private val vocabPracticeRepository: VocabPracticeRepository
) {

    /** Clears the stored meaning and returns how many cards were touched. */
    suspend fun refresh(): Int {
        val cardsToClear = vocabPracticeRepository.getAllCards()
            .filter { it.data.meaning != null && it.data.dictionaryId != null }

        if (cardsToClear.isEmpty()) return 0

        // Addressed by card id rather than through updateDeck: updateDeck needs the deck it belongs
        // to, and cards whose deck was deleted (the rows survive — SQLite does not cascade here) are
        // still cards that must be cleared, otherwise they come back as "refreshed" on every run.
        vocabPracticeRepository.clearCardMeanings(cardsToClear.map { it.cardId })

        return cardsToClear.size
    }

}
