package com.ppeternnguyen.myamiibo.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ppeternnguyen.myamiibo.MyAmiiboApplication
import com.ppeternnguyen.myamiibo.core.AmiiboDetail
import com.ppeternnguyen.myamiibo.data.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val detail: AmiiboDetail?) : DetailState
    data object Failed : DetailState
}

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CatalogRepository,
) : ViewModel() {
    val amiiboId: String = checkNotNull(savedStateHandle[ARG_ID])

    private val stateFlow = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = stateFlow.asStateFlow()

    init {
        load()
    }

    fun load() {
        stateFlow.value = DetailState.Loading
        viewModelScope.launch {
            stateFlow.value = try {
                DetailState.Loaded(repository.detail(amiiboId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailState.Failed
            }
        }
    }

    companion object {
        const val ARG_ID = "id"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyAmiiboApplication
                DetailViewModel(createSavedStateHandle(), app.container.catalogRepository)
            }
        }
    }
}
