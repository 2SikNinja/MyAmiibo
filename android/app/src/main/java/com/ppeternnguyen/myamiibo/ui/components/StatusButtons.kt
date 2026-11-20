package com.ppeternnguyen.myamiibo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ppeternnguyen.myamiibo.core.CollectionStatus
import com.ppeternnguyen.myamiibo.ui.theme.AmiiboColors

@Composable
fun StatusButtons(
    amiiboName: String,
    status: CollectionStatus?,
    onToggle: (CollectionStatus) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusButton(
            isActive = status == CollectionStatus.OWNED,
            activeLabel = "Owned",
            inactiveLabel = "Own it",
            activeIcon = Icons.Filled.CheckCircle,
            inactiveIcon = Icons.Outlined.CheckCircle,
            color = AmiiboColors.Owned,
            large = large,
            description = if (status == CollectionStatus.OWNED) {
                "Remove $amiiboName from owned"
            } else {
                "Mark $amiiboName as owned"
            },
            modifier = Modifier.weight(1f),
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle(CollectionStatus.OWNED)
        }
        StatusButton(
            isActive = status == CollectionStatus.WISHLIST,
            activeLabel = "Wishlist",
            inactiveLabel = "Want it",
            activeIcon = Icons.Filled.Star,
            inactiveIcon = Icons.Outlined.Star,
            color = AmiiboColors.Wishlist,
            large = large,
            description = if (status == CollectionStatus.WISHLIST) {
                "Remove $amiiboName from wishlist"
            } else {
                "Add $amiiboName to wishlist"
            },
            modifier = Modifier.weight(1f),
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle(CollectionStatus.WISHLIST)
        }
    }
}

@Composable
private fun StatusButton(
    isActive: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    color: Color,
    large: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val padding = if (large) PaddingValues(horizontal = 16.dp, vertical = 12.dp) else PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    val textStyle = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium
    val iconSize = if (large) 18.dp else 14.dp
    val semanticsModifier = modifier.semantics {
        contentDescription = description
        selected = isActive
    }
    val content: @Composable () -> Unit = {
        Icon(if (isActive) activeIcon else inactiveIcon, contentDescription = null, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(6.dp))
        Text(
            if (isActive) activeLabel else inactiveLabel,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (isActive) {
        Button(
            onClick = onClick,
            modifier = semanticsModifier,
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = semanticsModifier,
            contentPadding = padding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) { content() }
    }
}
