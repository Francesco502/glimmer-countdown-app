package com.example.timeapk.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

data class PermissionDialogSpec(
    val title: String,
    val message: String,
    val confirmText: String,
    val dismissText: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
    val onRequestDismiss: (() -> Unit)? = null
)

@Composable
fun PermissionActionDialog(spec: PermissionDialogSpec) {
    AlertDialog(
        onDismissRequest = spec.onRequestDismiss ?: spec.onDismiss,
        title = { Text(spec.title) },
        text = { Text(spec.message) },
        confirmButton = {
            TextButton(onClick = spec.onConfirm) {
                Text(spec.confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = spec.onDismiss) {
                Text(spec.dismissText)
            }
        }
    )
}
