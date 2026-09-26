package ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.tts.WordTtsManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.core.app_data.data.FuriganaString
import ua.syt0r.kanji.core.app_data.data.toKanaReading
import ua.syt0r.kanji.presentation.common.AutopaddedScrollableColumn
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.ui.CenteredBoxWithSide
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.FlashcardPracticeAnswerButtonsRow
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswers
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabReviewState

@Composable
fun VocabPracticeFlashcardUI(
    reviewState: VocabReviewState.Flashcard,
    answers: PracticeAnswers,
    onRevealAnswerClick: () -> Unit,
    onNextClick: (PracticeAnswer) -> Unit,
    onInfoClick: () -> Unit
) {

    AutopaddedScrollableColumn(
        modifier = Modifier.fillMaxSize(),
        bottomOverlayContent = {
            FlashcardPracticeAnswerButtonsRow(
                answers = answers,
                showAnswer = reviewState.showAnswer,
                onRevealAnswerClick = onRevealAnswerClick,
                onAnswerClick = onNextClick
            )
        }
    ) {

        val wordTts = koinInject<WordTtsManager>()
        val scope = rememberCoroutineScope()
        val readingText = reviewState.reading.toKanaReading()

        // Synthesis takes ~0.3–1.5s, so show that something is happening while it runs.
        var speaking by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                enabled = !speaking,
                onClick = {
                    scope.launch {
                        speaking = true
                        try {
                            wordTts.speak(readingText)
                        } finally {
                            speaking = false
                        }
                    }
                }
            ) {
                if (speaking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读")
                }
            }
        }

        val meaningUI = @Composable {
            CenteredBoxWithSide(
                modifier = Modifier.widthIn(max = 400.dp),
                placeSideContentAtStart = false,
                centerContent = {
                    Text(
                        text = reviewState.meaning,
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                sideContent = {
                    IconButton(
                        enabled = reviewState.showAnswer.value,
                        onClick = onInfoClick
                    ) {
                        Icon(Icons.Default.ArrowOutward, null)
                    }
                }
            )
        }

        val wordUI = @Composable { furigana: FuriganaString ->
            FuriganaText(
                furiganaString = furigana,
                textStyle = MaterialTheme.typography.displayLarge,
                annotationTextStyle = MaterialTheme.typography.bodyLarge
            )
        }

        val sentenceUI = @Composable { showTranslation: Boolean ->
            reviewState.exampleSentence?.let {
                Spacer(Modifier.height(Dimens.SpacingBig))
                SelectionContainer {
                    if (showTranslation) {
                        FuriganaText(
                            it.furigana,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.width(Dimens.ScreenWidth)
                        )
                    } else {
                        Text(
                            text = it.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(Dimens.ScreenWidth)
                        )
                    }
                }
                if (showTranslation) {
                    SelectionContainer {
                        Text(
                            text = it.translation,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(Dimens.ScreenWidth)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (reviewState.showMeaningInFront) {

                meaningUI()

                if (reviewState.showAnswer.value) {
                    Spacer(Modifier.height(8.dp))
                    wordUI(reviewState.reading)
                    sentenceUI(true)
                }

            } else {

                val text = reviewState.run { if (showAnswer.value) reading else noFuriganaReading }
                wordUI(text)

                if (reviewState.showAnswer.value) {
                    Spacer(Modifier.height(Dimens.SpacingMid))
                    meaningUI()
                }

                sentenceUI(reviewState.showAnswer.value)

            }

        }

    }

}
