package com.vladdrummer.prayerkmp.feature.contentlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import com.vladdrummer.prayerkmp.feature.contentlist.view_model.ContentListViewState
import kotlinproject.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import kotlinproject.composeapp.generated.resources.cs_icon
import com.vladdrummer.prayerkmp.feature.tableofcontents.PrayerData

@Composable
fun ContentListScreen(
    viewState: ContentListViewState,
    modifier: Modifier = Modifier,
    onPrayerClick: (PrayerData) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item{
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(viewState.items) { prayer ->
            Box {
                Card(
                    onClick = { onPrayerClick(prayer) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prayer.name.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } 
                if (prayer.chsResId != null) {
                    val badgeIconTint =
                        if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = MaterialTheme.shapes.small,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.cs_icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(badgeIconTint),
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
