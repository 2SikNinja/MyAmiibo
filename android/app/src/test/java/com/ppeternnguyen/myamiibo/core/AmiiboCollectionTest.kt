package com.ppeternnguyen.myamiibo.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AmiiboCollectionTest {
    @Test
    fun parseHandlesBadInput() {
        assertEquals(AmiiboCollection.EMPTY, AmiiboCollection.parse(null))
        assertEquals(AmiiboCollection.EMPTY, AmiiboCollection.parse("not json"))
        assertEquals(AmiiboCollection.EMPTY, AmiiboCollection.parse("[1,2]"))
    }

    @Test
    fun parseCleansIds() {
        val text = """
            { "owned": ["${Fixtures.MARIO_ID}", "${Fixtures.MARIO_ID}", "bad", 42],
              "wishlist": ["${Fixtures.MARIO_ID}", "${Fixtures.ISABELLE_ID}"] }
        """.trimIndent()
        val collection = AmiiboCollection.parse(text)
        assertEquals(listOf(Fixtures.MARIO_ID), collection.owned)
        assertEquals(listOf(Fixtures.ISABELLE_ID), collection.wishlist)
    }

    @Test
    fun backupFormatMatchesWebAndIos() {
        val collection = AmiiboCollection.of(listOf(Fixtures.MARIO_ID), listOf(Fixtures.ISABELLE_ID))
        assertEquals(
            """{"owned":["${Fixtures.MARIO_ID}"],"wishlist":["${Fixtures.ISABELLE_ID}"]}""",
            collection.encode(),
        )
        assertEquals(collection, AmiiboCollection.parse(collection.encode()))
    }

    @Test
    fun ownedAndWishlistAreExclusive() {
        val wished = AmiiboCollection.EMPTY.withStatus(Fixtures.MARIO_ID, CollectionStatus.WISHLIST)
        val owned = wished.withStatus(Fixtures.MARIO_ID, CollectionStatus.OWNED)
        assertEquals(listOf(Fixtures.MARIO_ID), owned.owned)
        assertEquals(emptyList<String>(), owned.wishlist)
        assertEquals(CollectionStatus.OWNED, owned.statusOf(Fixtures.MARIO_ID))
    }

    @Test
    fun togglingActiveStatusClearsIt() {
        val owned = AmiiboCollection.EMPTY.toggled(Fixtures.MARIO_ID, CollectionStatus.OWNED)
        assertEquals(AmiiboCollection.EMPTY, owned.toggled(Fixtures.MARIO_ID, CollectionStatus.OWNED))
    }

    @Test
    fun statsCountPerSeriesAndIgnoreUnknownIds() {
        val stats = CollectionStats.from(
            Fixtures.catalog,
            AmiiboCollection.of(listOf(Fixtures.ISABELLE_ID, "ffffffffffffffff"), listOf(Fixtures.MARIO_ID)),
        )
        assertEquals(1, stats.ownedCount)
        assertEquals(1, stats.wishlistCount)
        assertEquals(33, stats.percent)
        assertEquals(listOf(SeriesProgress("Animal Crossing", owned = 1, total = 2)), stats.series)
        assertEquals(0, stats.completedSeriesCount)
    }
}
