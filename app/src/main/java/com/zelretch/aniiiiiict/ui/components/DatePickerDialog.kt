package com.zelretch.aniiiiiict.ui.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import java.util.Calendar

@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDateTime) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // 日付の範囲制限を設定
    val minDate = Calendar.getInstance().apply {
        set(2000, Calendar.JANUARY, 1)
    }
    val maxDate = Calendar.getInstance().apply {
        add(Calendar.YEAR, 1) // 1年後まで
    }

    DisposableEffect(Unit) {
        val dialog = try {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    try {
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth)
                        }
                        
                        when {
                            selectedCalendar.before(minDate) -> {
                                onError("2000年1月1日以降の日付を選択してください")
                            }
                            selectedCalendar.after(maxDate) -> {
                                onError("1年以内の日付を選択してください")
                            }
                            else -> {
                                val selectedDate = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0)
                                onDateSelected(selectedDate)
                            }
                        }
                    } catch (e: Exception) {
                        onError("無効な日付が選択されました")
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnDismissListener { onDismiss() }
                datePicker.minDate = minDate.timeInMillis
                datePicker.maxDate = maxDate.timeInMillis
                show()
            }
        } catch (e: Exception) {
            onError("日付選択ダイアログの表示に失敗しました")
            null
        }

        onDispose {
            dialog?.dismiss()
        }
    }
} 