package com.vladdrummer.prayerkmp.feature.prayer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
actual fun PlatformPrayerText(
    html: String,
    fontSizeSp: Int,
    fontIndex: Int,
    modifier: Modifier,
) {
    Text(
        text = prayerHtmlToAnnotatedStringFallback(html),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSizeSp.sp),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}
