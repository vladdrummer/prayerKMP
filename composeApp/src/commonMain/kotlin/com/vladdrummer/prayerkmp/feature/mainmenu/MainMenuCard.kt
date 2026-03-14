package com.vladdrummer.prayerkmp.feature.mainmenu

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainMenuCard(
    item: MainMenuCardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val iconTint = if (isDarkTheme) Color.White else Color.Unspecified
    Card(
        onClick = onClick,
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        CardContent(item = item, iconTint = iconTint)
    }
}

@Composable
private fun CardContent(
    item: MainMenuCardItem,
    iconTint: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .defaultMinSize(minHeight = 156.dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = item.icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp)),
            tint = iconTint
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall.copy(lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = true
        )
    }
}
