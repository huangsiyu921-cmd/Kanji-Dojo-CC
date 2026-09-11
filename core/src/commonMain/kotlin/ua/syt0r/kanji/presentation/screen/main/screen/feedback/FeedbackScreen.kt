package ua.syt0r.kanji.presentation.screen.main.screen.feedback

import androidx.compose.runtime.Composable
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

@Composable
fun FeedbackScreen(
    feedbackTopic: FeedbackTopic,
    mainNavigationState: MainNavigationState
) {

    FeedbackScreenUI(
        navigateBack = { mainNavigationState.navigateBack() }
    )

}
