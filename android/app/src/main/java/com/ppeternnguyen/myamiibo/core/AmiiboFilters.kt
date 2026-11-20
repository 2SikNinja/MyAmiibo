package com.ppeternnguyen.myamiibo.core

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

enum class OwnershipFilter(val label: String) {
    ALL("All"),
    OWNED("Owned"),
    WISHLIST("Wishlist"),
    MISSING("Not owned"),
}

enum class SortOrder(val label: String) {
    NAME("Name (A to Z)"),
    NEWEST("Newest release"),
    OLDEST("Oldest release"),
}

data class AmiiboFilters(
    val query: String = "",
    /** null means any value. */
    val type: String? = "Figure",
    val amiiboSeries: String? = null,
    val gameSeries: String? = null,
    val ownership: OwnershipFilter = OwnershipFilter.ALL,
    val sort: SortOrder = SortOrder.NAME,
) {
    /** True when anything other than search text or sort differs from the defaults. */
    val hasActiveFilters: Boolean
        get() = type != DEFAULTS.type ||
            amiiboSeries != null ||
            gameSeries != null ||
            ownership != OwnershipFilter.ALL

    /** Changing the type clears series choices that may no longer exist. */
    fun withType(newType: String?): AmiiboFilters =
        if (newType == type) this else copy(type = newType, amiiboSeries = null, gameSeries = null)

    fun cleared(): AmiiboFilters = AmiiboFilters(query = query, sort = sort)

    companion object {
        val DEFAULTS = AmiiboFilters()
    }
}

object AmiiboSearch {
    private val nameCollator: Collator = Collator.getInstance(Locale.US).apply {
        strength = Collator.SECONDARY
    }

    private val diacritics = Regex("\\p{Mn}+")

    val byName: Comparator<Amiibo> = Comparator { a, b -> nameCollator.compare(a.name, b.name) }

    internal fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase(Locale.US)
            .trim()

    fun uniqueValues(list: List<Amiibo>, selector: (Amiibo) -> String): List<String> =
        list.map(selector).distinct().sortedWith { a, b -> nameCollator.compare(a, b) }

    fun filter(list: List<Amiibo>, filters: AmiiboFilters, collection: AmiiboCollection): List<Amiibo> {
        val terms = normalize(filters.query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        val ownedIds = collection.owned.toSet()
        val wishlistIds = collection.wishlist.toSet()

        val matches = list.filter { amiibo ->
            if (filters.type != null && amiibo.type != filters.type) return@filter false
            if (filters.amiiboSeries != null && amiibo.amiiboSeries != filters.amiiboSeries) return@filter false
            if (filters.gameSeries != null && amiibo.gameSeries != filters.gameSeries) return@filter false

            val ownershipMatches = when (filters.ownership) {
                OwnershipFilter.ALL -> true
                OwnershipFilter.OWNED -> amiibo.id in ownedIds
                OwnershipFilter.WISHLIST -> amiibo.id in wishlistIds
                OwnershipFilter.MISSING -> amiibo.id !in ownedIds
            }
            if (!ownershipMatches) return@filter false

            if (terms.isNotEmpty()) {
                val haystack = normalize(
                    "${amiibo.name} ${amiibo.character} ${amiibo.amiiboSeries} ${amiibo.gameSeries}",
                )
                if (!terms.all { haystack.contains(it) }) return@filter false
            }
            true
        }

        return sort(matches, filters.sort)
    }

    fun sort(list: List<Amiibo>, order: SortOrder): List<Amiibo> = when (order) {
        SortOrder.NAME -> list.sortedWith(byName)
        SortOrder.NEWEST, SortOrder.OLDEST -> {
            val newestFirst = order == SortOrder.NEWEST
            list.sortedWith { a, b ->
                val dateA = a.release.earliest
                val dateB = b.release.earliest
                if (dateA == dateB) {
                    byName.compare(a, b)
                } else if (dateA == null) {
                    // Unreleased or unknown dates always go last.
                    1
                } else if (dateB == null) {
                    -1
                } else if (newestFirst) {
                    dateB.compareTo(dateA)
                } else {
                    dateA.compareTo(dateB)
                }
            }
        }
    }
}
