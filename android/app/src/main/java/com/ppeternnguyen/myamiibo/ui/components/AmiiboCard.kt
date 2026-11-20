package com.ppeternnguyen.myamiibo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ppeternnguyen.myamiibo.core.Amiibo
import com.ppeternnguyen.myamiibo.core.CollectionStatus

@Composable
fun AmiiboCard(
    amiibo: Amiibo,
    status: CollectionStatus?,
    onOpen: () -> Unit,
    onToggle: (CollectionStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        // The clickable area and the status buttons are siblings so tapping a
        // button never also opens the detail screen.
        Column(
            modifier = Modifier
                .clickable(onClickLabel = "Open ${amiibo.name}", onClick = onOpen)
                .padding(start = 10.dp, end = 10.dp, top = 10.dp),
        ) {
            AmiiboImage(
                url = amiibo.imageUrl,
                contentDescription = amiibo.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
            )
            Text(
                amiibo.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                amiibo.amiiboSeries,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatusButtons(
            amiiboName = amiibo.name,
            status = status,
            onToggle = onToggle,
            modifier = Modifier.padding(10.dp),
        )
    }
}
