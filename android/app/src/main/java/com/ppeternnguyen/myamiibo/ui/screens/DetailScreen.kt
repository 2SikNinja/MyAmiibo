package com.ppeternnguyen.myamiibo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ppeternnguyen.myamiibo.core.Amiibo
import com.ppeternnguyen.myamiibo.core.CollectionStatus
import com.ppeternnguyen.myamiibo.core.GameUsage
import com.ppeternnguyen.myamiibo.ui.CatalogViewModel
import com.ppeternnguyen.myamiibo.ui.DetailState
import com.ppeternnguyen.myamiibo.ui.DetailViewModel
import com.ppeternnguyen.myamiibo.ui.components.AmiiboImage
import com.ppeternnguyen.myamiibo.ui.components.StatusButtons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

private fun formatDate(date: String?): String {
    if (date.isNullOrEmpty()) return "Not released"
    return try {
        LocalDate.parse(date).format(dateFormatter)
    } catch (e: DateTimeParseException) {
        date
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    catalogViewModel: CatalogViewModel,
    onBack: () -> Unit,
    detailViewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory),
) {
    val amiiboById by catalogViewModel.amiiboById.collectAsStateWithLifecycle()
    val collection by catalogViewModel.collection.collectAsStateWithLifecycle()
    val state by detailViewModel.state.collectAsStateWithLifecycle()

    // Show catalog data right away; the detail request adds game usage.
    val amiibo: Amiibo? = amiiboById[detailViewModel.amiiboId]
        ?: (state as? DetailState.Loaded)?.detail?.amiibo

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(amiibo?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val current = state
        if (amiibo == null) {
            when (current) {
                DetailState.Loading -> LoadingMessage("Loading amiibo…", Modifier.padding(padding))
                DetailState.Failed -> ErrorMessage(
                    title = "Couldn't load this amiibo",
                    message = "Check your connection and try again.",
                    onRetry = detailViewModel::load,
                    modifier = Modifier.padding(padding),
                )
                is DetailState.Loaded -> EmptyMessage(
                    title = "Amiibo not found",
                    message = "That amiibo doesn't exist or the link is broken.",
                    modifier = Modifier.padding(padding),
                )
            }
        } else {
            DetailContent(
                amiibo = amiibo,
                state = current,
                status = collection.statusOf(amiibo.id),
                onToggle = { status -> catalogViewModel.toggle(amiibo.id, status) },
                onRetryUsage = detailViewModel::load,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun DetailContent(
    amiibo: Amiibo,
    state: DetailState,
    status: CollectionStatus?,
    onToggle: (CollectionStatus) -> Unit,
    onRetryUsage: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AmiiboImage(
                url = amiibo.imageUrl,
                contentDescription = amiibo.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(20.dp),
            )
        }
        item {
            StatusButtons(
                amiiboName = amiibo.name,
                status = status,
                onToggle = onToggle,
                large = true,
            )
        }
        item {
            InfoCard(
                title = "Details",
                rows = listOf(
                    "Character" to amiibo.character,
                    "Amiibo series" to amiibo.amiiboSeries,
                    "Game series" to amiibo.gameSeries,
                    "Type" to amiibo.type,
                ),
            )
        }
        item {
            InfoCard(
                title = "Release dates",
                rows = amiibo.release.byRegion.map { it.region to formatDate(it.date) },
            )
        }

        when (state) {
            DetailState.Loading -> item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Loading games…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DetailState.Failed -> item {
                Column {
                    Text("Couldn't load game usage. Check your connection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onRetryUsage) { Text("Try again") }
                }
            }
            is DetailState.Loaded -> {
                val usage = state.detail?.usage.orEmpty()
                val gameCount = state.detail?.gameCount ?: 0
                item {
                    Text(
                        if (gameCount > 0) {
                            "Works with $gameCount ${if (gameCount == 1) "game" else "games"}"
                        } else {
                            "AmiiboAPI doesn't list any game features for this amiibo."
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(usage, key = { it.platform }) { platform ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${platform.platform} · ${platform.games.size} ${if (platform.games.size == 1) "game" else "games"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp),
                        )
                        platform.games.forEach { game ->
                            HorizontalDivider()
                            GameRow(game)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(16.dp),
        )
        rows.forEach { (label, value) ->
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(value, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GameRow(game: GameUsage) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(game.gameName, fontWeight = FontWeight.Medium)
        game.usages.forEach { usage ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    usage.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (usage.writesData) {
                    Text(
                        "SAVES DATA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
