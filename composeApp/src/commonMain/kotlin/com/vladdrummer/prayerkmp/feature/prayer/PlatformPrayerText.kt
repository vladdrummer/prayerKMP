package com.vladdrummer.prayerkmp.feature.prayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformPrayerText(
    html: String,
    fontSizeSp: Int,
    fontIndex: Int,
    modifier: Modifier = Modifier,
)
