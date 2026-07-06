package com.example.timeapk.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timeapk.R

@Composable
fun SongWheelPickerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    confirmText: String? = null,
    dismissText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedConfirmText = confirmText ?: stringResource(R.string.date_picker_ok)
    val resolvedDismissText = dismissText ?: stringResource(R.string.date_picker_cancel)

    SongFormDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        content = content,
        buttons = {
            SongDialogButton(text = resolvedDismissText, onClick = onDismissRequest)
            Spacer(modifier = Modifier.width(4.dp))
            SongDialogButton(
                text = resolvedConfirmText,
                onClick = onConfirm,
                enabled = confirmEnabled
            )
        }
    )
}
