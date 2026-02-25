package com.zelretch.aniiiiict.ui.animedetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.annict.WorkDetailQuery
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.ui.base.UiState
import com.zelretch.aniiiiict.ui.common.components.StatusDropdown

private const val IMAGE_SIZE = 120
private const val CARD_ELEVATION = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    programWithWork: ProgramWithWork,
    onNavigateBack: () -> Unit,
    viewModel: AnimeDetailViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel<AnimeDetailViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(programWithWork) {
        viewModel.loadAnimeDetail(programWithWork)
    }

    Scaffold(
        topBar = {
            AnimeDetailTopAppBar(
                title = programWithWork.work.title,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("loading_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is com.zelretch.aniiiiict.ui.base.UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is UiState.Success -> {
                    AnimeDetailContent(
                        animeDetailInfo = state.data.animeDetailInfo,
                        selectedStatus = state.data.selectedStatus,
                        isStatusChanging = state.data.isStatusChanging,
                        statusChangeError = state.data.statusChangeError,
                        onStatusChange = viewModel::changeStatus,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeDetailContent(
    animeDetailInfo: AnimeDetailInfo,
    selectedStatus: StatusState?,
    isStatusChanging: Boolean,
    statusChangeError: String?,
    onStatusChange: (StatusState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ヘッダー情報
        AnimeDetailHeader(
            animeDetailInfo = animeDetailInfo,
            selectedStatus = selectedStatus,
            isStatusChanging = isStatusChanging,
            statusChangeError = statusChangeError,
            onStatusChange = onStatusChange
        )

        // 基本情報
        AnimeDetailBasicInfo(animeDetailInfo = animeDetailInfo)

        // 外部リンク
        AnimeDetailExternalLinks(animeDetailInfo = animeDetailInfo)

        // 配信プラットフォーム
        animeDetailInfo.programs?.let { programs ->
            if (programs.isNotEmpty()) {
                AnimeDetailPrograms(programs = programs)
            }
        }

        // 関連作品
        animeDetailInfo.seriesList?.let { seriesList ->
            if (seriesList.isNotEmpty()) {
                AnimeDetailRelatedWorks(seriesList = seriesList)
            }
        }
    }
}

@Composable
private fun AnimeDetailHeader(
    animeDetailInfo: AnimeDetailInfo,
    selectedStatus: StatusState?,
    isStatusChanging: Boolean,
    statusChangeError: String?,
    onStatusChange: (StatusState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 画像
            AsyncImage(
                model = animeDetailInfo.imageUrl,
                contentDescription = animeDetailInfo.work.title,
                modifier = Modifier.size(IMAGE_SIZE.dp),
                contentScale = ContentScale.Crop
            )

            // 基本情報
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = animeDetailInfo.work.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                animeDetailInfo.work.seasonNameText?.let { seasonText ->
                    Text(
                        text = seasonText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = animeDetailInfo.episodeCount?.let { "全${it}話" } ?: "全?話",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ステータス変更
        StatusDropdown(
            selectedStatus = selectedStatus,
            isChanging = isStatusChanging,
            onStatusChange = onStatusChange
        )

        statusChangeError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AnimeDetailBasicInfo(animeDetailInfo: AnimeDetailInfo, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "基本情報",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            animeDetailInfo.work.mediaText?.let { mediaText ->
                InfoRow(label = "メディア", value = mediaText)
            }

            animeDetailInfo.malInfo?.status?.let { status ->
                InfoRow(label = "放送状況", value = status)
            }

            animeDetailInfo.malInfo?.mediaType?.let { mediaType ->
                InfoRow(label = "種別", value = mediaType)
            }

            animeDetailInfo.malInfo?.broadcast?.let { broadcast ->
                val broadcastText = buildString {
                    broadcast.dayOfTheWeek?.let { append("$it ") }
                    broadcast.time?.let { append(it) }
                }
                if (broadcastText.isNotEmpty()) {
                    InfoRow(label = "放送時間", value = broadcastText)
                }
            }
        }
    }
}

@Composable
private fun AnimeDetailExternalLinks(animeDetailInfo: AnimeDetailInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val links = listOfNotNull(
        animeDetailInfo.officialSiteUrl?.let { "公式サイト" to it },
        animeDetailInfo.wikipediaUrl?.let { "Wikipedia" to it }
    )

    if (links.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "外部リンク",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                links.forEach { (label, url) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeDetailPrograms(programs: List<WorkDetailQuery.Node1?>, modifier: Modifier = Modifier) {
    // チャンネル名の重複を排除
    val uniqueChannels = programs
        .filterNotNull()
        .map { it.channel.name }
        .distinct()
        .sorted()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "配信プラットフォーム",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            uniqueChannels.forEach { channelName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeDetailRelatedWorks(seriesList: List<WorkDetailQuery.Node2?>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION.dp)
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

            seriesList.filterNotNull().forEach { series ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = series.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    series.works?.nodes?.filterNotNull()?.forEach { work ->
                        Text(
                            text = "  • ${work.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeDetailTopAppBar(title: String, onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
