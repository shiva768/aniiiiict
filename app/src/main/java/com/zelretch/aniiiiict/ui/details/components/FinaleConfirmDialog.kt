package com.zelretch.aniiiiict.ui.details.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun FinaleConfirmDialog(episodeNumber: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("最終話確認") },
        text = {
            Text("第${episodeNumber}話は最終話です。\n作品のステータスを「視聴完了」に変更しますか？")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("視聴完了にする")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("後で")
            }
        }
    )
}
