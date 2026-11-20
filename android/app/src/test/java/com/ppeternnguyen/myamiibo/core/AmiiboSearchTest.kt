package com.ppeternnguyen.myamiibo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmiiboSearchTest {
    private val anyType = AmiiboFilters(type = null)

    private fun names(filters: AmiiboFilters, collection: AmiiboCollection = AmiiboCollection.EMPTY): List<String> =
        AmiiboSearch.filter(Fixtures.catalog, filters, collection).map { it.name }

    @Test
    fun defaultsToFiguresOnly() {
        assertEquals(listOf("Isabelle - Winter Outfit", "Mario"), names(AmiiboFilters.DEFAULTS))
    }

    @Test
    fun searchIgnoresCaseAndAccents() {
        assertEquals(listOf("Céleste"), names(anyType.copy(query = "CELESTE")))
        assertEquals(listOf("Mario"), names(anyType.copy(query = "smash mario")))
        assertEquals(listOf("Céleste"), names(anyType.copy(query = "pokemon trainer")))
    }

    @Test
    fun seriesFilters() {
        assertEquals(2, names(anyType.copy(amiiboSeries = "Animal Crossing")).size)
        assertEquals(listOf("Mario"), names(anyType.copy(gameSeries = "Super Mario")))
    }

    @Test
    fun ownershipFilters() {
        val collection = AmiiboCollection.of(listOf(Fixtures.MARIO_ID), listOf(Fixtures.CARD_ID))
        assertEquals(listOf("Mario"), names(anyType.copy(ownership = OwnershipFilter.OWNED), collection))
        assertEquals(listOf("Céleste"), names(anyType.copy(ownership = OwnershipFilter.WISHLIST), collection))
        assertEquals(
            listOf("Céleste", "Isabelle - Winter Outfit"),
            names(anyType.copy(ownership = OwnershipFilter.MISSING), collection),
        )
    }

    @Test
    fun releaseSortPutsUnreleasedLast() {
        assertEquals(
            listOf("Isabelle - Winter Outfit", "Mario", "Céleste"),
            names(anyType.copy(sort = SortOrder.NEWEST)),
        )
        assertEquals(
            listOf("Mario", "Isabelle - Winter Outfit", "Céleste"),
            names(anyType.copy(sort = SortOrder.OLDEST)),
        )
    }

    @Test
    fun uniqueValuesAreSorted() {
        assertEquals(listOf("Card", "Figure"), AmiiboSearch.uniqueValues(Fixtures.catalog) { it.type })
    }

    @Test
    fun changingTypeClearsSeries() {
        val filters = AmiiboFilters(amiiboSeries = "Animal Crossing", gameSeries = "Animal Crossing")
        val changed = filters.withType("Card")
        assertEquals("Card", changed.type)
        assertEquals(null, changed.amiiboSeries)
        assertEquals(filters, filters.withType("Figure"))
    }

    @Test
    fun hasActiveFilters() {
        assertFalse(AmiiboFilters.DEFAULTS.hasActiveFilters)
        assertTrue(anyType.hasActiveFilters)
        assertFalse(AmiiboFilters(query = "mario", sort = SortOrder.NEWEST).hasActiveFilters)
    }
}
