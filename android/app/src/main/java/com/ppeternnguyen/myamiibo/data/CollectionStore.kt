package com.ppeternnguyen.myamiibo.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.ppeternnguyen.myamiibo.core.AmiiboCollection
import com.ppeternnguyen.myamiibo.core.CollectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The user's owned and wishlist amiibo, saved on this device. */
class CollectionStore(private val prefs: SharedPreferences) {
    private val state = MutableStateFlow(AmiiboCollection.parse(prefs.getString(KEY, null)))

    val collection: StateFlow<AmiiboCollection> = state.asStateFlow()

    fun toggle(id: String, status: CollectionStatus) {
        state.update { it.toggled(id, status) }
        persist()
    }

    /** Replaces the collection with a backup. Returns false when the backup has no amiibo. */
    fun importBackup(text: String): Boolean {
        val imported = AmiiboCollection.parse(text)
        if (imported.isEmpty) return false
        state.value = imported
        persist()
        return true
    }

    fun exportBackup(): String = state.value.encode()

    private fun persist() {
        prefs.edit { putString(KEY, state.value.encode()) }
    }

    private companion object {
        const val KEY = "amiibo-collection-v1"
    }
}
