package com.ppeternnguyen.myamiibo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppeternnguyen.myamiibo.core.Amiibo
import com.ppeternnguyen.myamiibo.core.AmiiboCollection
import com.ppeternnguyen.myamiibo.core.AmiiboSearch
import com.ppeternnguyen.myamiibo.core.CollectionStatus
import com.ppeternnguyen.myamiibo.core.SeriesProgress
import com.ppeternnguyen.myamiibo.ui.CatalogViewModel
import com.ppeternnguyen.myamiibo.ui.components.AmiiboImage
import com.ppeternnguyen.myamiibo.ui.theme.AmiiboColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(viewModel: CatalogViewModel, onOpenAmiibo: (String) -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val amiiboById by viewModel.amiiboById.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(CollectionStatus.OWNED) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun applyImport(text: String) {
        val imported = AmiiboCollection.parse(text)
        viewModel.importBackup(text)
        scope.launch {
            snackbarHostState.showSnackbar(
                "Imported ${imported.owned.size} owned and ${imported.wishlist.size} wishlist amiibo.",
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val backup = viewModel.exportBackup()
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(backup.toByteArray()) }
                }.isSuccess
            }
            snackbarHostState.showSnackbar(if (saved) "Backup saved." else "Couldn't save the backup.")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            if (text == null) {
                snackbarHostState.showSnackbar("Couldn't read that file.")
            } else if (AmiiboCollection.parse(text).isEmpty) {
                snackbarHostState.showSnackbar("That file has no amiibo in it, so nothing changed.")
            } else if (collection.isEmpty) {
                applyImport(text)
            } else {
                pendingImport = text
            }
        }
    }

    val tabItems = remember(selectedTab, collection, amiiboById) {
        val ids = if (selectedTab == CollectionStatus.OWNED) collection.owned else collection.wishlist
        ids.mapNotNull { amiiboById[it] }.sortedWith(AmiiboSearch.byName)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Collection") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Backup options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Export backup") }, onClick = {
                            menuExpanded = false
                            exportLauncher.launch("amiibo-collection.json")
                        })
                        DropdownMenuItem(text = { Text("Import backup") }, onClick = {
                            menuExpanded = false
                            importLauncher.launch(arrayOf("application/json", "text/plain"))
                        })
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Owned", stats.ownedCount, "${stats.percent}% of ${stats.totalCount}", Modifier.weight(1f))
                    StatTile("Wishlist", stats.wishlistCount, "to find", Modifier.weight(1f))
                    StatTile("Series", stats.series.size, "${stats.completedSeriesCount} complete", Modifier.weight(1f))
                }
            }

            if (stats.series.isNotEmpty()) {
                item {
                    Text(
                        "Series progress",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(stats.series, key = { "series-${it.series}" }) { entry -> SeriesRow(entry) }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    FilterChip(
                        selected = selectedTab == CollectionStatus.OWNED,
                        onClick = { selectedTab = CollectionStatus.OWNED },
                        label = { Text("Owned (${stats.ownedCount})") },
                    )
                    FilterChip(
                        selected = selectedTab == CollectionStatus.WISHLIST,
                        onClick = { selectedTab = CollectionStatus.WISHLIST },
                        label = { Text("Wishlist (${stats.wishlistCount})") },
                    )
                }
            }

            if (tabItems.isEmpty()) {
                item {
                    Text(
                        if (selectedTab == CollectionStatus.OWNED) {
                            "You haven't marked any amiibo as owned yet. Tap Own it on any amiibo in Browse."
                        } else {
                            "Your wishlist is empty. Tap Want it on any amiibo in Browse."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                items(tabItems, key = { it.id }) { amiibo ->
                    CollectionRow(
                        amiibo = amiibo,
                        onOpen = { onOpenAmiibo(amiibo.id) },
                        onRemove = { viewModel.toggle(amiibo.id, selectedTab) },
                    )
                }
            }
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace your collection?") },
            text = { Text("The backup will replace the amiibo currently on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    applyImport(text)
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SeriesRow(entry: SeriesProgress) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text(
                    entry.series,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${entry.owned}/${entry.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { entry.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = if (entry.isComplete) AmiiboColors.Owned else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CollectionRow(amiibo: Amiibo, onOpen: () -> Unit, onRemove: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable(onClickLabel = "Open ${amiibo.name}", onClick = onOpen),
            leadingContent = {
                AmiiboImage(url = amiibo.imageUrl, contentDescription = null, modifier = Modifier.size(48.dp))
            },
            headlineContent = { Text(amiibo.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(amiibo.amiiboSeries, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingContent = {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Clear, contentDescription = "Remove ${amiibo.name}")
                }
            },
        )
    }
}
