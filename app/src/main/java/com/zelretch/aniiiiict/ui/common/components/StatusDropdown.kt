package com.zelretch.aniiiiict.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.annict.type.StatusState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StatusDropdown(
    selectedStatus: StatusState?,
    isChanging: Boolean,
    onStatusChange: (StatusState) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded && !isChanging,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedStatus?.toJapaneseLabel() ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = !isChanging,
                trailingIcon = {
                    if (isChanging) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = expanded && !isChanging,
                onDismissRequest = { expanded = false }
            ) {
                StatusState.entries.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status.toJapaneseLabel()) },
                        onClick = {
                            expanded = false
                            onStatusChange(status)
                        }
                    )
                }
            }
        }
    }
}
