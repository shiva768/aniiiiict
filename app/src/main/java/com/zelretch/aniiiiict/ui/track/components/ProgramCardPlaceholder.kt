package com.zelretch.aniiiiict.ui.track.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiict.ui.base.shimmer

private const val PLACEHOLDER_TITLE_WIDTH_FRACTION = 0.75f
private const val PLACEHOLDER_DATE_WIDTH_FRACTION = 0.45f

@Composable
fun ProgramCardPlaceholder(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 作品情報セクション
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shimmer(cornerRadius = 8.dp, isLoading = true)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .shimmer(cornerRadius = 4.dp, isLoading = true)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(PLACEHOLDER_TITLE_WIDTH_FRACTION)
                            .height(18.dp)
                            .shimmer(cornerRadius = 4.dp, isLoading = true)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(22.dp)
                                .shimmer(cornerRadius = 6.dp, isLoading = true)
                        )
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(22.dp)
                                .shimmer(cornerRadius = 6.dp, isLoading = true)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // エピソード情報セクション
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(PLACEHOLDER_TITLE_WIDTH_FRACTION)
                        .height(16.dp)
                        .shimmer(cornerRadius = 4.dp, isLoading = true)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(PLACEHOLDER_DATE_WIDTH_FRACTION)
                        .height(12.dp)
                        .shimmer(cornerRadius = 4.dp, isLoading = true)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .shimmer(cornerRadius = 10.dp, isLoading = true)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .shimmer(cornerRadius = 10.dp, isLoading = true)
                    )
                }
            }
        }
    }
}
