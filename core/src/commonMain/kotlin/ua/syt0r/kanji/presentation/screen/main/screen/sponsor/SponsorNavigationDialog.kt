package ua.syt0r.kanji.presentation.screen.main.screen.sponsor

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.resolveString

/**
 * Asks before leaving for the sponsor screen. The handshake button is easy to hit by accident, and
 * the sponsor screen is not part of studying, so it gets a confirmation.
 */
@Composable
fun SponsorNavigationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(resolveString { sponsor.dialogTitle }) },
        content = { Text(resolveString { sponsor.dialogMessage }) },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(resolveString { sponsor.dialogCancel })
            }
            TextButton(onClick = onConfirm) {
                Text(resolveString { sponsor.dialogConfirm })
            }
        }
    )

}
