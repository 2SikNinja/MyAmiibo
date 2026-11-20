package com.ppeternnguyen.myamiibo.data

import com.ppeternnguyen.myamiibo.core.Amiibo
import com.ppeternnguyen.myamiibo.core.AmiiboDecoding
import com.ppeternnguyen.myamiibo.core.AmiiboDetail
import com.ppeternnguyen.myamiibo.core.AmiiboSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class CatalogSnapshot(val catalog: List<Amiibo>, val updatedAtMillis: Long)

/**
 * The amiibo catalog. The raw API response is cached on disk so the app opens
 * instantly and works offline; it refreshes once a day.
 */
class CatalogRepository(
    private val api: AmiiboApi,
    private val cacheFile: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val detailCache = ConcurrentHashMap<String, AmiiboDetail>()

    suspend fun cached(): CatalogSnapshot? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        val catalog = runCatching { AmiiboDecoding.decodeList(cacheFile.readText()) }.getOrNull()
        if (catalog.isNullOrEmpty()) null else CatalogSnapshot(sortedByName(catalog), cacheFile.lastModified())
    }

    fun isStale(snapshot: CatalogSnapshot): Boolean =
        now() - snapshot.updatedAtMillis > TimeUnit.DAYS.toMillis(1)

    suspend fun refresh(): CatalogSnapshot {
        val text = api.fetchCatalogText()
        val catalog = sortedByName(AmiiboDecoding.decodeList(text))
        withContext(Dispatchers.IO) {
            val temp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
            temp.writeText(text)
            temp.renameTo(cacheFile)
        }
        return CatalogSnapshot(catalog, now())
    }

    suspend fun detail(id: String): AmiiboDetail? {
        detailCache[id]?.let { return it }
        return api.fetchDetail(id)?.also { detailCache[id] = it }
    }

    private fun sortedByName(list: List<Amiibo>): List<Amiibo> = list.sortedWith(AmiiboSearch.byName)
}
