package com.ppeternnguyen.myamiibo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmiiboDecodingTest {
    @Test
    fun listSkipsDuplicatesAndInvalidEntries() {
        val list = AmiiboDecoding.decodeList(Fixtures.listJson)
        assertEquals(listOf("Mario", "Isabelle - Winter Outfit", "Céleste"), list.map { it.name })
        assertEquals(Fixtures.MARIO_ID, list.first().id)
    }

    @Test
    fun releaseDatesKeepNulls() {
        val isabelle = Fixtures.catalog.first { it.id == Fixtures.ISABELLE_ID }
        assertEquals(ReleaseDates(na = "2015-11-13", eu = "2016-03-18"), isabelle.release)
        assertEquals("2015-11-13", isabelle.release.earliest)
    }

    @Test
    fun detailOrdersPlatformsNewestFirstAndSkipsEmptyOnes() {
        val detail = AmiiboDecoding.decodeDetail(Fixtures.marioDetailJson)!!
        assertEquals(
            listOf("Nintendo Switch 2", "Nintendo Switch", "Nintendo 3DS"),
            detail.usage.map { it.platform },
        )
        assertEquals(4, detail.gameCount)
    }

    @Test
    fun detailSortsGamesAndCleansUsageText() {
        val detail = AmiiboDecoding.decodeDetail(Fixtures.marioDetailJson)!!
        val switchGames = detail.usage[1].games
        assertEquals(listOf("Super Mario Odyssey", "Super Smash Bros. Ultimate"), switchGames.map { it.gameName })
        assertEquals(listOf(UsageEntry("Battle a figure player", writesData = true)), switchGames[1].usages)
        assertEquals(listOf(UsageEntry("Use a tennis ball design", writesData = false)), detail.usage[0].games[0].usages)
    }

    @Test
    fun detailPicksTheRequestedIdFromAList() {
        val text = """{ "amiibo": [${Fixtures.isabelleJson}, ${Fixtures.marioJson}] }"""
        assertEquals("Mario", AmiiboDecoding.decodeDetail(text, Fixtures.MARIO_ID)?.amiibo?.name)
        assertNull(AmiiboDecoding.decodeDetail(text, "ffffffffffffffff"))
    }

    @Test
    fun badInputGivesEmptyResults() {
        assertTrue(AmiiboDecoding.decodeList("""{"code":404}""").isEmpty())
        assertTrue(AmiiboDecoding.decodeList("not json").isEmpty())
        assertNull(AmiiboDecoding.decodeDetail("[]"))
    }
}
