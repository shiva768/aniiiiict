package com.zelretch.aniiiiict.ui.track.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.annict.type.StatusState
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.ui.common.components.toJapaneseLabel

private const val YEAR_COLLAPSE_AFTER = 4
private const val CHANNEL_COLLAPSE_AFTER = 6

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

/**
 * 適用中フィルターの数（検索クエリは別表示なので含めない）。AppBarのバッジに使う。
 */
fun FilterState.appliedCount(): Int =
    selectedMedia.size + selectedSeason.size + selectedYear.size + selectedChannel.size + selectedStatus.size

/**
 * 検索・フィルターを1枚に集約したボトムシート。チップはドラフトを編集し、
 * 「N件を表示」で結果数を事前提示してから適用する。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    filterOptions: FilterOptions,
    previewCount: (FilterState) -> Int,
    onApply: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(filterState) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = draft.searchQuery,
                onValueChange = { draft = draft.copy(searchQuery = it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("作品名やチャンネル名で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "検索") },
                trailingIcon = {
                    if (draft.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { draft = draft.copy(searchQuery = "") }) {
                            Icon(Icons.Default.Clear, contentDescription = "クリア")
                        }
                    }
                },
                singleLine = true
            )

            if (filterOptions.media.isNotEmpty()) {
                ChipSection(
                    title = "メディア",
                    values = filterOptions.media,
                    selected = draft.selectedMedia,
                    label = { it },
                    onToggle = { draft = draft.copy(selectedMedia = draft.selectedMedia.toggle(it)) }
                )
            }
            if (filterOptions.seasons.isNotEmpty()) {
                ChipSection(
                    title = "シーズン",
                    values = filterOptions.seasons,
                    selected = draft.selectedSeason,
                    label = { it.name },
                    onToggle = {
                        draft =
                            draft.copy(selectedSeason = draft.selectedSeason.toggle(it))
                    }
                )
            }
            if (filterOptions.years.isNotEmpty()) {
                // 新しい順＋直近のみ表示、選択中は常に表示
                ChipSection(
                    title = "年",
                    values = filterOptions.years.sortedDescending(),
                    selected = draft.selectedYear,
                    label = { "${it}年" },
                    collapseAfter = YEAR_COLLAPSE_AFTER,
                    onToggle = { draft = draft.copy(selectedYear = draft.selectedYear.toggle(it)) }
                )
            }
            if (filterOptions.channels.isNotEmpty()) {
                ChipSection(
                    title = "チャンネル",
                    values = filterOptions.channels,
                    selected = draft.selectedChannel,
                    label = { it },
                    collapseAfter = CHANNEL_COLLAPSE_AFTER,
                    onToggle = { draft = draft.copy(selectedChannel = draft.selectedChannel.toggle(it)) }
                )
            }
            ChipSection(
                title = "ステータス",
                values = listOf(StatusState.WATCHING, StatusState.WANNA_WATCH),
                selected = draft.selectedStatus,
                label = { it.toJapaneseLabel() },
                onToggle = { draft = draft.copy(selectedStatus = draft.selectedStatus.toggle(it)) }
            )

            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text("${previewCount(draft)}件を表示")
            }
        }
    }
}

/**
 * 非使用時はゼロ占有。使用中のみリスト上部に1行：検索チップ（×で即解除）＋選択値チップ。
 * チップタップでシート再開。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterSummaryRow(filterState: FilterState, onOpenSheet: () -> Unit, onClearSearch: () -> Unit) {
    val hasSearch = filterState.searchQuery.isNotEmpty()
    if (!hasSearch && !filterState.hasActiveNonSearchFilters()) return

    val valueLabels = buildList {
        filterState.selectedStatus.forEach { add(it.toJapaneseLabel()) }
        filterState.selectedMedia.forEach { add(it) }
        filterState.selectedSeason.forEach { add(it.name) }
        filterState.selectedYear.sortedDescending().forEach { add("${it}年") }
        filterState.selectedChannel.forEach { add(it) }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (hasSearch) {
            InputChip(
                selected = true,
                onClick = onOpenSheet,
                label = { Text("🔍 ${filterState.searchQuery}") },
                trailingIcon = {
                    IconButton(onClick = onClearSearch, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "検索を解除", modifier = Modifier.size(16.dp))
                    }
                }
            )
        }
        valueLabels.forEach { label ->
            SuggestionChip(onClick = onOpenSheet, label = { Text(label) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipSection(
    title: String,
    values: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    collapseAfter: Int = Int.MAX_VALUE
) {
    var expanded by remember { mutableStateOf(false) }
    // 折りたたみ中でも選択済みの値は必ず表示する
    val visible = if (expanded || values.size <= collapseAfter) {
        values
    } else {
        (values.take(collapseAfter) + values.filter { it in selected }).distinct()
    }
    val hiddenCount = values.size - visible.size

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            visible.forEach { value ->
                FilterChip(
                    selected = value in selected,
                    onClick = { onToggle(value) },
                    label = { Text(label(value)) }
                )
            }
            if (hiddenCount > 0) {
                SuggestionChip(
                    onClick = { expanded = true },
                    label = { Text("+$hiddenCount すべて表示") }
                )
            }
        }
    }
}
