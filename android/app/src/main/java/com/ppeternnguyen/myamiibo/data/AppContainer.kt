package com.ppeternnguyen.myamiibo.data

import android.content.Context
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Creates the app's long-lived objects once, at startup. */
class AppContainer(context: Context) {
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    val catalogRepository = CatalogRepository(
        api = AmiiboApi(httpClient),
        cacheFile = File(context.cacheDir, "amiibo-catalog.json"),
    )

    val collectionStore = CollectionStore(
        context.getSharedPreferences("collection", Context.MODE_PRIVATE),
    )
}
