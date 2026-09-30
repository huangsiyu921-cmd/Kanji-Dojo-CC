package ua.syt0r.kanji.presentation.screen.main.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.sponsor.SponsorNavigationDialog

private const val GitHubLink = "https://github.com/huangsiyu921-cmd/Kanji-Dojo-CC"

@Composable
fun HomeScreen(
    mainNavigationState: MainNavigationState,
    viewModel: HomeScreenContract.ViewModel = getMultiplatformViewModel(),
) {

    val homeNavigationState = rememberHomeNavigationState(viewModel.defaultTab)

    val tabContent = remember {
        movableContentOf { HomeNavigationContent(homeNavigationState, mainNavigationState) }
    }

    val uriHandler = LocalUriHandler.current

    var showSponsorDialog by remember { mutableStateOf(false) }

    HomeScreenUI(
        availableTabs = HomeScreenTab.VisibleTabs,
        selectedTabState = homeNavigationState.selectedTab,
        onTabSelected = { homeNavigationState.navigate(it) },
        onGitHubClick = { uriHandler.openUri(GitHubLink) },
        // The handshake sits next to the study tabs and is easy to hit by accident, so ask first.
        onSponsorButtonClick = { showSponsorDialog = true }
    ) {

        tabContent()

    }

    if (showSponsorDialog) {
        SponsorNavigationDialog(
            onDismissRequest = { showSponsorDialog = false },
            onConfirm = {
                showSponsorDialog = false
                mainNavigationState.navigate(MainDestination.Sponsor)
            }
        )
    }

    val analyticsManager = koinInject<AnalyticsManager>()
    LaunchedEffect(Unit) {
        snapshotFlow { homeNavigationState.selectedTab.value }
            .distinctUntilChanged()
            .onEach { analyticsManager.setScreen(it.analyticsName) }
            .launchIn(this)
    }

}