package com.ppeternnguyen.myamiibo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ppeternnguyen.myamiibo.MyAmiiboApplication
import com.ppeternnguyen.myamiibo.core.Amiibo
import com.ppeternnguyen.myamiibo.core.AmiiboCollection
import com.ppeternnguyen.myamiibo.core.AmiiboFilters
import com.ppeternnguyen.myamiibo.core.AmiiboSearch
import com.ppeternnguyen.myamiibo.core.CollectionStats
import com.ppeternnguyen.myamiibo.core.CollectionStatus
import com.ppeternnguyen.myamiibo.data.CatalogRepository
import com.ppeternnguyen.myamiibo.data.CollectionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Failed(val message: String) : CatalogState
    data object Loaded : CatalogState
}

/** Shared by the browse and collection screens. */
class CatalogViewModel(
    private val repository: CatalogRepository,
    private val collectionStore: CollectionStore,
) : ViewModel() {
    private val catalogFlow = MutableStateFlow<List<Amiibo>>(emptyList())
    val catalog: StateFlow<List<Amiibo>> = catalogFlow.asStateFlow()

    private val stateFlow = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val state: StateFlow<CatalogState> = stateFlow.asStateFlow()

    private val refreshingFlow = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = refreshingFlow.asStateFlow()

    private val filtersFlow = MutableStateFlow(AmiiboFilters.DEFAULTS)
    val filters: StateFlow<AmiiboFilters> = filtersFlow.asStateFlow()

    val collection: StateFlow<AmiiboCollection> = collectionStore.collection

    val results: StateFlow<List<Amiibo>> =
        combine(catalogFlow, filtersFlow, collection) { catalog, filters, collection ->
            AmiiboSearch.filter(catalog, filters, collection)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<CollectionStats> =
        combine(catalogFlow, collection) { catalog, collection -> CollectionStats.from(catalog, collection) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CollectionStats.from(emptyList(), AmiiboCollection.EMPTY),
            )

    val amiiboById: StateFlow<Map<String, Amiibo>> =
        catalogFlow.map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        viewModelScope.launch { load() }
    }

    /** Shows the cached catalog right away, then refreshes when it's stale. */
    private suspend fun load() {
        val cached = repository.cached()
        if (cached != null) {
            catalogFlow.value = cached.catalog
            stateFlow.value = CatalogState.Loaded
            if (!repository.isStale(cached)) return
        }
        refreshNow()
    }

    fun refresh() {
        viewModelScope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        if (refreshingFlow.value) return
        refreshingFlow.value = true
        if (catalogFlow.value.isEmpty()) stateFlow.value = CatalogState.Loading
        try {
            catalogFlow.value = repository.refresh().catalog
            stateFlow.value = CatalogState.Loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep showing cached data if there is any. Only surface the error
            // when there is nothing to show.
            if (catalogFlow.value.isEmpty()) {
                stateFlow.value = CatalogState.Failed(e.message ?: "Couldn't reach AmiiboAPI.")
            }
        } finally {
            refreshingFlow.value = false
        }
    }

    fun updateFilters(transform: (AmiiboFilters) -> AmiiboFilters) {
        filtersFlow.update(transform)
    }

    fun toggle(id: String, status: CollectionStatus) {
        collectionStore.toggle(id, status)
    }

    fun importBackup(text: String): Boolean = collectionStore.importBackup(text)

    fun exportBackup(): String = collectionStore.exportBackup()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyAmiiboApplication
                CatalogViewModel(app.container.catalogRepository, app.container.collectionStore)
            }
        }
    }
}
