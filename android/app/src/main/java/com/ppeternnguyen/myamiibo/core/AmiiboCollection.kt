package com.ppeternnguyen.myamiibo.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/**
 * The user's owned and wishlist amiibo ids. Owned and wishlist are exclusive.
 * Create instances with [of] so ids are always cleaned.
 */
class AmiiboCollection private constructor(
    val owned: List<String>,
    val wishlist: List<String>,
) {
    val isEmpty: Boolean
        get() = owned.isEmpty() && wishlist.isEmpty()

    fun statusOf(id: String): CollectionStatus? = when (id) {
        in owned -> CollectionStatus.OWNED
        in wishlist -> CollectionStatus.WISHLIST
        else -> null
    }

    /** Sets a status. Passing null removes the amiibo from both lists. */
    fun withStatus(id: String, status: CollectionStatus?): AmiiboCollection {
        val newOwned = owned.filter { it != id }.toMutableList()
        val newWishlist = wishlist.filter { it != id }.toMutableList()
        when (status) {
            CollectionStatus.OWNED -> newOwned += id
            CollectionStatus.WISHLIST -> newWishlist += id
            null -> Unit
        }
        return of(newOwned, newWishlist)
    }

    /** Tapping the active status clears it; tapping the other one switches. */
    fun toggled(id: String, status: CollectionStatus): AmiiboCollection =
        withStatus(id, if (statusOf(id) == status) null else status)

    /** JSON in the same format as the web and iOS backups. */
    fun encode(): String = buildJsonObject {
        putJsonArray("owned") { owned.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("wishlist") { wishlist.forEach { add(JsonPrimitive(it)) } }
    }.toString()

    override fun equals(other: Any?): Boolean =
        other is AmiiboCollection && other.owned == owned && other.wishlist == wishlist

    override fun hashCode(): Int = 31 * owned.hashCode() + wishlist.hashCode()

    override fun toString(): String = "AmiiboCollection(owned=$owned, wishlist=$wishlist)"

    companion object {
        val EMPTY = AmiiboCollection(emptyList(), emptyList())

        private val idPattern = Regex("^[0-9a-fA-F]{16}$")

        fun isValidId(id: String): Boolean = idPattern.matches(id)

        fun of(owned: List<String>, wishlist: List<String>): AmiiboCollection {
            val cleanOwned = owned.filter(::isValidId).distinct()
            val ownedSet = cleanOwned.toSet()
            val cleanWishlist = wishlist.filter(::isValidId).distinct().filter { it !in ownedSet }
            return AmiiboCollection(cleanOwned, cleanWishlist)
        }

        /** Parses stored or imported JSON. Never throws; bad input gives an empty collection. */
        fun parse(text: String?): AmiiboCollection {
            if (text.isNullOrBlank()) return EMPTY
            val root = runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonObject
                ?: return EMPTY
            fun ids(key: String): List<String> =
                (root[key] as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }
                    .orEmpty()
            return of(ids("owned"), ids("wishlist"))
        }
    }
}

