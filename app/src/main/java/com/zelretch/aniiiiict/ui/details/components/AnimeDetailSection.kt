package com.zelretch.aniiiiict.ui.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.RelatedSeries
import com.zelretch.aniiiiict.data.model.RelatedWork
import com.zelretch.aniiiiict.data.model.StreamingPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailSection(
    animeDetailInfo: AnimeDetailInfo,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Episode Count Information
        item {
            EpisodeCountSection(animeDetailInfo)
        }

        // Streaming Platforms
        if (animeDetailInfo.streamingPlatforms.isNotEmpty()) {
            item {
                StreamingPlatformsSection(animeDetailInfo.streamingPlatforms)
            }
        }

        // External Links
        item {
            ExternalLinksSection(animeDetailInfo)
        }

        // Statistics
        item {
            StatisticsSection(animeDetailInfo)
        }

        // Related Works
        if (animeDetailInfo.relatedSeries.isNotEmpty()) {
            item {
                RelatedWorksSection(animeDetailInfo.relatedSeries)
            }
        }
    }
}

@Composable
private fun EpisodeCountSection(animeDetailInfo: AnimeDetailInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "エピソード情報",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Annict episode count
                Column {
                    Text(
                        text = "Annict",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (animeDetailInfo.noEpisodes) "話数未定" else "${animeDetailInfo.episodesCount ?: "不明"}話",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // MyAnimeList episode count
                if (animeDetailInfo.malEpisodeCount != null) {
                    Column {
                        Text(
                            text = "MyAnimeList",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${animeDetailInfo.malEpisodeCount}話",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingPlatformsSection(platforms: List<StreamingPlatform>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "配信プラットフォーム",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            platforms.forEach { platform ->
                PlatformItem(platform)
            }
        }
    }
}

@Composable
private fun PlatformItem(platform: StreamingPlatform) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = platform.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        if (platform.channelGroup != null) {
            Text(
                text = platform.channelGroup,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (platform.isRebroadcast) {
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = "再放送",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
private fun ExternalLinksSection(animeDetailInfo: AnimeDetailInfo) {
    val uriHandler = LocalUriHandler.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "外部リンク",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            animeDetailInfo.officialSiteUrl?.let { url ->
                ExternalLinkItem(
                    title = "公式サイト",
                    url = url,
                    onClick = { uriHandler.openUri(url) }
                )
            }
            
            animeDetailInfo.wikipediaUrl?.let { url ->
                ExternalLinkItem(
                    title = "Wikipedia",
                    url = url,
                    onClick = { uriHandler.openUri(url) }
                )
            }
            
            if (animeDetailInfo.twitterUsername != null) {
                ExternalLinkItem(
                    title = "Twitter",
                    url = "https://twitter.com/${animeDetailInfo.twitterUsername}",
                    onClick = { uriHandler.openUri("https://twitter.com/${animeDetailInfo.twitterUsername}") }
                )
            }
        }
    }
}

@Composable
private fun ExternalLinkItem(
    title: String,
    url: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "開く",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatisticsSection(animeDetailInfo: AnimeDetailInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "統計情報",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    icon = Icons.Default.Visibility,
                    label = "視聴者数",
                    value = "${animeDetailInfo.watchersCount ?: 0}人"
                )
                
                StatisticItem(
                    icon = Icons.Default.Star,
                    label = "レビュー数",
                    value = "${animeDetailInfo.reviewsCount ?: 0}件"
                )
                
                if (animeDetailInfo.satisfactionRate != null) {
                    StatisticItem(
                        icon = Icons.Default.Star,
                        label = "満足度",
                        value = "${String.format("%.1f", animeDetailInfo.satisfactionRate)}%"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RelatedWorksSection(relatedSeries: List<RelatedSeries>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "関連作品",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            relatedSeries.forEach { series ->
                RelatedSeriesItem(series)
            }
        }
    }
}

@Composable
private fun RelatedSeriesItem(series: RelatedSeries) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = series.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(series.works) { work ->
                RelatedWorkItem(work)
            }
        }
    }
}

@Composable
private fun RelatedWorkItem(work: RelatedWork) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            work.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = work.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Text(
                text = work.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (work.seasonName != null && work.seasonYear != null) {
                Text(
                    text = "${work.seasonYear}年 ${work.seasonName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}