package com.ppeternnguyen.myamiibo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppeternnguyen.myamiibo.core.AmiiboFilters
import com.ppeternnguyen.myamiibo.ui.CatalogState
import com.ppeternnguyen.myamiibo.ui.CatalogViewModel
import com.ppeternnguyen.myamiibo.ui.components.AmiiboCard
import com.ppeternnguyen.myamiibo.ui.components.FilterSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(viewModel: CatalogViewModel, onOpenAmiibo: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("MyAmiibo") },
                actions = {
                    IconButton(onClick = { showFilters = true }, enabled = catalog.isNotEmpty()) {
                        BadgedBox(badge = { if (filters.hasActiveFilters) Badge() }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Filters")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = { query -> viewModel.updateFilters { it.copy(query = query) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Name, character, or series") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateFilters { it.copy(query = "") } }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            when (val current = state) {
                CatalogState.Loading -> LoadingMessage("Loading amiibo…")
                is CatalogState.Failed -> ErrorMessage(
                    title = "Couldn't load amiibo",
                    message = current.message,
                    onRetry = viewModel::refresh,
                )
                CatalogState.Loaded -> PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (results.isEmpty()) {
                        EmptyMessage(
                            title = "No matches",
                            message = if (filters.query.isNotEmpty()) {
                                "No amiibo match \"${filters.query}\"."
                            } else {
                                "No amiibo match these filters."
                            },
                        ) {
                            TextButton(onClick = { viewModel.updateFilters { AmiiboFilters(sort = it.sort) } }) {
                                Text("Reset filters")
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            state = gridState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "${results.size} ${if (results.size == 1) "result" else "results"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            items(results, key = { it.id }) { amiibo ->
                                AmiiboCard(
                                    amiibo = amiibo,
                                    status = collection.statusOf(amiibo.id),
                                    onOpen = { onOpenAmiibo(amiibo.id) },
                                    onToggle = { status -> viewModel.toggle(amiibo.id, status) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            filters = filters,
            catalog = catalog,
            onChange = viewModel::updateFilters,
            onDismiss = { showFilters = false },
        )
    }
}

