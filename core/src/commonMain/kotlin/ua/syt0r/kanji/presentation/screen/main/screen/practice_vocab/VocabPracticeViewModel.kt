package ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.app_data.data.toKanaReading
import ua.syt0r.kanji.core.tts.WordTtsManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCardsSelectorState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.VocabPracticeScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.MutableVocabReviewState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.SelectedReadingAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeQueueState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeReviewState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.use_case.GetVocabPracticeQueueDataUseCase

class VocabPracticeViewModel(
    private val viewModelScope: CoroutineScope,
    private val practicePreferences: PreferencesContract.PracticePreferences,
    private val getQueueDataUseCase: GetVocabPracticeQueueDataUseCase,
    private val practiceQueue: VocabPracticeQueue,
    private val analyticsManager: AnalyticsManager,
    private val wordTtsManager: WordTtsManager
) : VocabPracticeScreenContract.ViewModel {

    private lateinit var configuration: VocabPracticeScreenConfiguration

    private lateinit var _reviewState: MutableState<VocabPracticeQueueState.Review>
    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)

    /** The auto-play collector of the card currently on screen; replaced on every new card. */
    private var autoReadJob: Job? = null

    override val state: StateFlow<ScreenState>
        get() = _state

    override fun initialize(configuration: VocabPracticeScreenConfiguration) {
        if (this::configuration.isInitialized) return
        this.configuration = configuration

        viewModelScope.launch {

            val selectorState = PracticeConfigurationCardsSelectorState(
                cardsCount = configuration.cards.size,
                shuffle = mutableStateOf(practicePreferences.shuffle.get()),
                newCardsOrder = mutableStateOf(practicePreferences.newCardsOrder.get())
            )

            _state.value = ScreenState.Configuration(
                practiceType = configuration.practiceType,
                cardsSelectorState = selectorState,
                flashcard = VocabPracticeConfiguration.Flashcard(
                    translationInFront = mutableStateOf(
                        practicePreferences.vocabFlashcardMeaningInFront.get()
                    )
                ),
                readingPicker = VocabPracticeConfiguration.ReadingPicker(
                    showMeaning = mutableStateOf(
                        practicePreferences.vocabReadingPickerShowMeaning.get()
                    )
                ),
                writing = VocabPracticeConfiguration.Writing(
                    showKanaReading = mutableStateOf(
                        practicePreferences.vocabWritingShowKanaReading.get()
                    )
                )
            )
        }
    }

    override fun configure() {
        val configurationState = _state.value as? ScreenState.Configuration ?: return
        _state.value = ScreenState.Loading

        viewModelScope.launch {
            practicePreferences.apply {
                shuffle.set(configurationState.cardsSelectorState.shuffle.value)
                newCardsOrder.set(configurationState.cardsSelectorState.newCardsOrder.value)
                vocabReadingPickerShowMeaning.set(configurationState.readingPicker.showMeaning.value)
                vocabFlashcardMeaningInFront.set(configurationState.flashcard.translationInFront.value)
                vocabWritingShowKanaReading.set(configurationState.writing.showKanaReading.value)
            }

            practiceQueue.initialize(
                items = getQueueDataUseCase(configuration, configurationState)
            )

            practiceQueue.state
                .onEach { applyToScreenState(it) }
                .launchIn(viewModelScope)

            analyticsManager.sendEvent("vocab_practice_configuration") {
                put("practice_type", configuration.practiceType.dataType.srsPracticeType.value)
            }
        }
    }

    override fun revealFlashcard() {
        val currentState = _reviewState.value.state as MutableVocabReviewState.Flashcard
        currentState.showAnswer.value = true
    }

    override fun submitReadingPickerAnswer(answer: String) {
        val currentState = _reviewState.value.state as MutableVocabReviewState.Reading
        currentState.apply {
            displayReading.value = currentState.revealedReading
            selectedAnswer.value = SelectedReadingAnswer(answer, currentState.correctAnswer)
        }
    }

    override fun next(answer: PracticeAnswer) {
        viewModelScope.launch { practiceQueue.submitAnswer(answer) }
    }

    override fun finishPractice() {
        practiceQueue.immediateFinish()
    }

    private fun applyToScreenState(queueState: VocabPracticeQueueState) {
        when (queueState) {
            VocabPracticeQueueState.Loading -> {
                _state.value = ScreenState.Loading
            }

            is VocabPracticeQueueState.Review -> {
                if (::_reviewState.isInitialized.not()) {
                    _reviewState = mutableStateOf(queueState)
                } else {
                    _reviewState.value = queueState
                }

                if (_state.value !is ScreenState.Review) {
                    _state.value = ScreenState.Review(
                        state = derivedStateOf { _reviewState.value.toPracticeReviewState() }
                    )
                }

                // Replacing the job matters as much as launching it: an older flow keeps watching
                // its own card's state, so without cancelling it a stale card could speak again
                // later (observed as "it talks when I finish the practice").
                autoReadJob?.cancel()
                autoReadJob = queueState.autoReadFlow()
                    .onEach { wordTtsManager.speak(it) }
                    .launchIn(viewModelScope)
            }

            is VocabPracticeQueueState.Summary -> {
                _state.value = ScreenState.Summary(
                    practiceDuration = queueState.duration,
                    results = queueState.items
                )
            }
        }
    }

    private fun VocabPracticeQueueState.Review.toPracticeReviewState(): VocabPracticeReviewState {
        return VocabPracticeReviewState(
            progress = progress,
            reviewState = state.asImmutable,
            answers = answers
        )
    }

    /**
     * Speaks a card's kana reading once the word is actually on screen, mirroring the auto play of
     * the kana practice screen. The reading picker is the exception: there the reading *is* the
     * answer, so it is only spoken after the answer has been revealed.
     */
    private fun VocabPracticeQueueState.Review.autoReadFlow(): Flow<String> = callbackFlow {
        when (val state = this@autoReadFlow.state) {
            is MutableVocabReviewState.Flashcard -> {
                // The word is visible from the start, unless the meaning is shown on the front.
                snapshotFlow { state.showAnswer.value || !state.showMeaningInFront }
                    .filter { it }
                    .take(1)
                    .onEach { send(state.reading.toKanaReading()) }
                    .collect()
            }

            is MutableVocabReviewState.Reading -> {
                snapshotFlow { state.selectedAnswer.value }
                    .filterNotNull()
                    .take(1)
                    .onEach { send(state.revealedReading.toKanaReading()) }
                    .collect()
            }

            is MutableVocabReviewState.Writing -> {
                send(state.kanaReading)
            }
        }
        awaitClose()
    }

}