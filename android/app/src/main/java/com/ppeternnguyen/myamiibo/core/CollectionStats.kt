package com.ppeternnguyen.myamiibo.core

import kotlin.math.roundToInt

data class SeriesProgress(val series: String, val owned: Int, val total: Int) {
    val percent: Int
        get() = if (total == 0) 0 else (owned * 100.0 / total).roundToInt()

    val fraction: Float
        get() = if (total == 0) 0f else owned.toFloat() / total

    val isComplete: Boolean
        get() = total > 0 && owned == total
}

data class CollectionStats(
    val ownedCount: Int,
    val wishlistCount: Int,
    val totalCount: Int,
    /** Only series where the user owns at least one amiibo, most complete first. */
    val series: List<SeriesProgress>,
) {
    val percent: Int
        get() = if (totalCount == 0) 0 else (ownedCount * 100.0 / totalCount).roundToInt()

    val completedSeriesCount: Int
        get() = series.count { it.isComplete }

    companion object {
        fun from(catalog: List<Amiibo>, collection: AmiiboCollection): CollectionStats {
            val ownedIds = collection.owned.toSet()
            val knownIds = catalog.mapTo(HashSet()) { it.id }

            val series = catalog
                .groupBy { it.amiiboSeries }
                .map { (name, members) ->
                    SeriesProgress(name, owned = members.count { it.id in ownedIds }, total = members.size)
                }
                .filter { it.owned > 0 }
                .sortedWith(
                    compareByDescending<SeriesProgress> { it.percent }
                        .thenByDescending { it.owned }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.series },
                )

            return CollectionStats(
                ownedCount = collection.owned.count { it in knownIds },
                wishlistCount = collection.wishlist.count { it in knownIds },
                totalCount = catalog.size,
                series = series,
            )
        }
    }
}
