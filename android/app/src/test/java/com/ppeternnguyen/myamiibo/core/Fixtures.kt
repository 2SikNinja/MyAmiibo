package com.ppeternnguyen.myamiibo.core

object Fixtures {
    val marioJson = """
        {
          "amiiboSeries": "Super Smash Bros.",
          "character": "Mario",
          "gameSeries": "Super Mario",
          "head": "00000000",
          "tail": "00000002",
          "image": "https://example.com/mario.png",
          "name": "Mario",
          "type": "Figure",
          "release": { "au": "2014-11-29", "eu": "2014-11-28", "jp": "2014-12-06", "na": "2014-11-21" },
          "games3DS": [
            { "gameID": ["0004000000123400"], "gameName": "Super Smash Bros. for Nintendo 3DS",
              "amiiboUsage": [{ "Usage": "Train a figure player", "write": true }] }
          ],
          "gamesSwitch": [
            { "gameID": ["01006A800016E000"], "gameName": "Super Smash Bros. Ultimate",
              "amiiboUsage": [{ "Usage": "Battle a figure player", "write": true }] },
            { "gameID": ["0100000000010000"], "gameName": "Super Mario Odyssey",
              "amiiboUsage": [{ "Usage": "Unlock Mario's Builder Outfit", "write": false }] }
          ],
          "gamesSwitch2": [
            { "gameID": ["0400D4201C6EA000"], "gameName": "Mario Tennis Fever",
              "amiiboUsage": [{ "Usage": "  Use a tennis ball design  ", "write": false }, { "Usage": "" }] }
          ],
          "gamesWiiU": []
        }
    """.trimIndent()

    val isabelleJson = """
        {
          "amiiboSeries": "Animal Crossing",
          "character": "Isabelle",
          "gameSeries": "Animal Crossing",
          "head": "01810000",
          "tail": "02430502",
          "image": "https://example.com/isabelle.png",
          "name": "Isabelle - Winter Outfit",
          "type": "Figure",
          "release": { "au": null, "eu": "2016-03-18", "jp": null, "na": "2015-11-13" }
        }
    """.trimIndent()

    val cardJson = """
        {
          "amiiboSeries": "Animal Crossing",
          "character": "Pokémon Trainer",
          "gameSeries": "Animal Crossing",
          "head": "01830000",
          "tail": "02510502",
          "image": "https://example.com/card.png",
          "name": "Céleste",
          "type": "Card",
          "release": { "au": null, "eu": null, "jp": null, "na": null }
        }
    """.trimIndent()

    const val BROKEN_JSON = """{ "name": "Broken" }"""

    val listJson = """{ "amiibo": [$marioJson, $marioJson, $BROKEN_JSON, $isabelleJson, $cardJson] }"""

    val marioDetailJson = """{ "amiibo": $marioJson }"""

    const val MARIO_ID = "0000000000000002"
    const val ISABELLE_ID = "0181000002430502"
    const val CARD_ID = "0183000002510502"

    val catalog: List<Amiibo> by lazy { AmiiboDecoding.decodeList(listJson) }
}
