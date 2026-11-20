package com.ppeternnguyen.myamiibo.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Turns raw AmiiboAPI JSON into app models. Bad entries are skipped rather
 * than failing the whole response.
 */
object AmiiboDecoding {
    private val json = Json { ignoreUnknownKeys = true }

    private val platformLabels = mapOf(
        "games3DS" to "Nintendo 3DS",
        "gamesWiiU" to "Wii U",
        "gamesSwitch" to "Nintendo Switch",
        "gamesSwitch2" to "Nintendo Switch 2",
    )

    private val platformOrder = listOf("gamesSwitch2", "gamesSwitch", "gamesWiiU", "games3DS")

    /** Every amiibo in a catalog response, without duplicates. */
    fun decodeList(text: String): List<Amiibo> {
        val seenIds = mutableSetOf<String>()
        return entries(text).mapNotNull { raw ->
            makeAmiibo(raw)?.takeIf { seenIds.add(it.id) }
        }
    }

    /**
     * One amiibo with game usage. Lookups can return a single object or an
     * array. When an id is given, only the entry with that id is used.
     */
    fun decodeDetail(text: String, id: String? = null): AmiiboDetail? {
        for (raw in entries(text)) {
            val amiibo = makeAmiibo(raw) ?: continue
            if (id != null && !amiibo.id.equals(id, ignoreCase = true)) continue
            return AmiiboDetail(amiibo, makeUsage(raw))
        }
        return null
    }

    private fun entries(text: String): List<JsonObject> {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: return emptyList()
        return when (val amiibo = root["amiibo"]) {
            is JsonArray -> amiibo.filterIsInstance<JsonObject>()
            is JsonObject -> listOf(amiibo)
            else -> emptyList()
        }
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun makeAmiibo(raw: JsonObject): Amiibo? {
        val head = raw.string("head")?.takeIf { it.isNotEmpty() } ?: return null
        val tail = raw.string("tail")?.takeIf { it.isNotEmpty() } ?: return null
        val name = raw.string("name")?.takeIf { it.isNotEmpty() } ?: return null
        val release = raw["release"] as? JsonObject
        return Amiibo(
            id = head + tail,
            name = name,
            character = raw.string("character") ?: name,
            amiiboSeries = raw.string("amiiboSeries") ?: "Unknown",
            gameSeries = raw.string("gameSeries") ?: "Unknown",
            type = raw.string("type") ?: "Unknown",
            imageUrl = raw.string("image"),
            release = ReleaseDates(
                na = release?.string("na"),
                eu = release?.string("eu"),
                jp = release?.string("jp"),
                au = release?.string("au"),
            ),
        )
    }

    private fun makeUsage(raw: JsonObject): List<PlatformUsage> {
        fun rank(key: String): Int = platformOrder.indexOf(key).takeIf { it >= 0 } ?: 99

        return raw.entries
            .filter { (key, value) -> key.startsWith("games") && value is JsonArray }
            .sortedWith(compareBy<Map.Entry<String, *>>({ rank(it.key) }, { it.key }))
            .mapNotNull { (key, value) ->
                val games = (value as JsonArray)
                    .filterIsInstance<JsonObject>()
                    .mapNotNull { game -> makeGame(game) }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.gameName })
                if (games.isEmpty()) return@mapNotNull null
                PlatformUsage(platform = platformLabels[key] ?: key.removePrefix("games"), games = games)
            }
    }

    private fun makeGame(game: JsonObject): GameUsage? {
        val gameName = game.string("gameName") ?: return null
        val usages = (game["amiiboUsage"] as? JsonArray)
            ?.filterIsInstance<JsonObject>()
            ?.mapNotNull { usage ->
                val text = usage.string("Usage")?.trim().orEmpty()
                if (text.isEmpty()) {
                    null
                } else {
                    UsageEntry(
                        description = text,
                        writesData = (usage["write"] as? JsonPrimitive)?.booleanOrNull ?: false,
                    )
                }
            }
            .orEmpty()
        return GameUsage(gameName, usages)
    }
}
