package com.zelretch.aniiiiiict.ui.unwatched.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    episodeNumber: Int,
    episodeCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ここまでまとめて視聴済みにする") },
        text = { Text("第${episodeNumber}話まで、合計${episodeCount}話を視聴済みにします。\nこの操作は取り消せません。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("視聴済みにする")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
} 