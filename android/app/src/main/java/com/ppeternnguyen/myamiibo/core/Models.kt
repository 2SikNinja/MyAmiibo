package com.ppeternnguyen.myamiibo.core

data class ReleaseDates(
    val na: String? = null,
    val eu: String? = null,
    val jp: String? = null,
    val au: String? = null,
) {
    /** Earliest known release date across regions, as an ISO date string. */
    val earliest: String?
        get() = listOfNotNull(na, eu, jp, au).filter { it.isNotEmpty() }.minOrNull()

    /** Regions in display order, paired with a readable label. */
    val byRegion: List<RegionRelease>
        get() = listOf(
            RegionRelease("North America", na),
            RegionRelease("Europe", eu),
            RegionRelease("Japan", jp),
            RegionRelease("Australia", au),
        )
}

data class RegionRelease(val region: String, val date: String?)

data class Amiibo(
    /** head + tail from AmiiboAPI. Unique per amiibo. */
    val id: String,
    val name: String,
    val character: String,
    val amiiboSeries: String,
    val gameSeries: String,
    val type: String,
    val imageUrl: String?,
    val release: ReleaseDates,
)

data class UsageEntry(val description: String, val writesData: Boolean)

data class GameUsage(val gameName: String, val usages: List<UsageEntry>)

data class PlatformUsage(val platform: String, val games: List<GameUsage>)

data class AmiiboDetail(val amiibo: Amiibo, val usage: List<PlatformUsage>) {
    val gameCount: Int
        get() = usage.sumOf { it.games.size }
}

enum class CollectionStatus { OWNED, WISHLIST }
