package com.zelretch.aniiiiict.ui.library

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.zelretch.aniiiiict.data.model.WorkPriority
import com.zelretch.aniiiiict.ui.common.components.toJapaneseLabel
import timber.log.Timber

private const val QR_SIZE_PX = 768
private const val QR_MARGIN = 2

/** 優先度を QR で見せるダイアログ。QR に入りきらないときはテキスト共有に案内する */
@Composable
internal fun PriorityExportDialog(state: PriorityTransferState.Export, onShareText: () -> Unit, onDismiss: () -> Unit) {
    val qr = remember(state.payload) {
        runCatching { createQrBitmap(state.payload) }
            .onFailure { Timber.w(it, "優先度の QR を作れない: ${state.payload.length}文字") }
            .getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("優先度を QR で渡す") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("別の端末のライブラリで「QR を読み込む」から読み取ってください。")
                TierCountsText(state.counts)
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "優先度の QR コード",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            // ダークテーマでも読み取れるよう白地に載せる
                            .background(ComposeColor.White)
                            .testTag("priority_export_qr")
                    )
                } else {
                    Text(
                        text = "作品が多すぎて QR に入りきりません。テキストで共有してください。",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
        dismissButton = { TextButton(onClick = onShareText) { Text("テキストで共有") } }
    )
}

/** 読み取った優先度で置き換えてよいかの確認 */
@Composable
internal fun PriorityImportConfirmDialog(
    state: PriorityTransferState.ConfirmImport,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("優先度を置き換えますか？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TierCountsText(state.counts)
                Text("この端末の優先度はすべて、読み取った内容に置き換わります。")
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("置き換える") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@Composable
private fun TierCountsText(counts: Map<WorkPriority, Int>) {
    Text(
        text = counts.entries.joinToString(" / ") { (tier, count) -> "${tier.toJapaneseLabel()} ${count}件" },
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .testTag("priority_transfer_counts")
    )
}

private fun createQrBitmap(text: String): Bitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
        EncodeHintType.MARGIN to QR_MARGIN,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX, hints)
    val pixels = IntArray(matrix.width * matrix.height) { i ->
        if (matrix[i % matrix.width, i / matrix.width]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}

/** 優先度の文字列を共有シートで送る（QR が読みにくいとき用） */
internal fun sharePriorityText(context: Context, payload: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, payload)
    }
    context.startActivity(Intent.createChooser(intent, "優先度を共有"))
}

/** クリップボードの文字列（無ければ null） */
internal fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    return clipboard?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

/** Google Code Scanner で QR を読む。カメラ権限は不要（スキャナー側の画面で撮る） */
internal fun scanPriorityQr(context: Context, onScanned: (String) -> Unit, onError: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    GmsBarcodeScanning.getClient(context, options)
        .startScan()
        .addOnSuccessListener { barcode ->
            barcode.rawValue?.let(onScanned) ?: onError("QR を読み取れませんでした")
        }
        .addOnFailureListener { e ->
            Timber.w(e, "QR の読み取りに失敗")
            onError("QR を読み取れませんでした")
        }
}
