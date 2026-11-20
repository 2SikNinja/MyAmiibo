package com.ppeternnguyen.myamiibo.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ppeternnguyen.myamiibo.core.Amiibo
import com.ppeternnguyen.myamiibo.core.AmiiboFilters
import com.ppeternnguyen.myamiibo.core.AmiiboSearch
import com.ppeternnguyen.myamiibo.core.OwnershipFilter
import com.ppeternnguyen.myamiibo.core.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    filters: AmiiboFilters,
    catalog: List<Amiibo>,
    onChange: ((AmiiboFilters) -> AmiiboFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val types = remember(catalog) { AmiiboSearch.uniqueValues(catalog) { it.type } }
    // Series pickers only list values that exist for the chosen type.
    val typeScoped = remember(catalog, filters.type) {
        filters.type?.let { type -> catalog.filter { it.type == type } } ?: catalog
    }
    val amiiboSeries = remember(typeScoped) { AmiiboSearch.uniqueValues(typeScoped) { it.amiiboSeries } }
    val gameSeries = remember(typeScoped) { AmiiboSearch.uniqueValues(typeScoped) { it.gameSeries } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)

            FilterDropdown("Type", filters.type, "All types", types) { value ->
                onChange { it.withType(value) }
            }
            FilterDropdown("Amiibo series", filters.amiiboSeries, "All series", amiiboSeries) { value ->
                onChange { it.copy(amiiboSeries = value) }
            }
            FilterDropdown("Game series", filters.gameSeries, "All game series", gameSeries) { value ->
                onChange { it.copy(gameSeries = value) }
            }

            ChipGroup("Collection") {
                OwnershipFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filters.ownership == option,
                        onClick = { onChange { it.copy(ownership = option) } },
                        label = { Text(option.label) },
                    )
                }
            }
            ChipGroup("Sort") {
                SortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = filters.sort == order,
                        onClick = { onChange { it.copy(sort = order) } },
                        label = { Text(order.label) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = { onChange { it.cleared() } }, enabled = filters.hasActiveFilters) {
                    Text("Reset")
                }
                Button(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun ChipGroup(label: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { chips() }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    value: String?,
    allLabel: String,
    options: List<String>,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value ?: allLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 420.dp),
        ) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = {
                expanded = false
                onSelect(null)
            })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    expanded = false
                    onSelect(option)
                })
            }
        }
    }
}
