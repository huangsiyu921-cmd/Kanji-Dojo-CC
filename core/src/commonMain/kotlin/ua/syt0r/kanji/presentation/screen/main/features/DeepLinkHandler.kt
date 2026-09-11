package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import ua.syt0r.kanji.core.launchWhenHasSubscribers
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

class DeepLinkHandler(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
) {

    private val _deepLinksFlow = MutableSharedFlow<String>()
    val deepLinksFlow: SharedFlow<String> = _deepLinksFlow

    fun notifyDeepLink(link: String) {
        _deepLinksFlow.launchWhenHasSubscribers(coroutineScope) { _deepLinksFlow.emit(link) }
    }

    @Composable
    fun HandleDeepLinksLaunchedEffect(navigationState: MainNavigationState) {
        LaunchedEffect(Unit) {
            deepLinksFlow.collectLatest {
                Logger.d("Unsupported deeplink[$it]")
            }
        }
    }

}
