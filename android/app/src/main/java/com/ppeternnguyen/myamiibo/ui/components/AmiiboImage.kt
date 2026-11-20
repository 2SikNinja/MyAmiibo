package com.ppeternnguyen.myamiibo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/** Amiibo artwork from AmiiboAPI. */
@Composable
fun AmiiboImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
