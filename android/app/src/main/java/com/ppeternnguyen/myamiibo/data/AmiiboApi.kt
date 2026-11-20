package com.ppeternnguyen.myamiibo.data

import com.ppeternnguyen.myamiibo.core.AmiiboCollection
import com.ppeternnguyen.myamiibo.core.AmiiboDecoding
import com.ppeternnguyen.myamiibo.core.AmiiboDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class AmiiboApiException(message: String) : IOException(message)

/** Client for https://www.amiiboapi.org. */
class AmiiboApi(
    private val client: OkHttpClient,
    baseUrl: String = "https://www.amiiboapi.org/api",
) {
    private val amiiboUrl: HttpUrl = baseUrl.toHttpUrl().newBuilder()
        .addPathSegment("amiibo")
        .addPathSegment("")
        .build()

    /** Raw catalog JSON, so it can be cached on disk as-is. */
    suspend fun fetchCatalogText(): String {
        val text = get(amiiboUrl) ?: throw AmiiboApiException("AmiiboAPI returned 404 for the catalog.")
        if (AmiiboDecoding.decodeList(text).isEmpty()) {
            throw AmiiboApiException("AmiiboAPI returned no amiibo.")
        }
        return text
    }

    /** One amiibo with game usage, or null when the id is unknown. */
    suspend fun fetchDetail(id: String): AmiiboDetail? {
        if (!AmiiboCollection.isValidId(id)) return null
        // AmiiboAPI ignores showusage on ?id= lookups, so query by head and tail.
        val url = amiiboUrl.newBuilder()
            .addQueryParameter("head", id.take(8))
            .addQueryParameter("tail", id.takeLast(8))
            .addQueryParameter("showusage", null)
            .build()
        val text = get(url) ?: return null
        return AmiiboDecoding.decodeDetail(text, id)
    }

    /** Body text, or null on 404. */
    private suspend fun get(url: HttpUrl): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> throw AmiiboApiException("AmiiboAPI responded with status ${response.code}.")
                else -> response.body?.string().orEmpty()
            }
        }
    }
}
