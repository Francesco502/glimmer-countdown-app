package com.example.timeapk.ui.components

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
    SongConfirmDialog(
        title = spec.title,
        message = spec.message,
        confirmText = spec.confirmText,
        dismissText = spec.dismissText,
        onConfirm = spec.onConfirm,
        onDismiss = spec.onDismiss,
        onDismissRequest = spec.onRequestDismiss ?: spec.onDismiss
    )
}
