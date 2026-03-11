package com.vladdrummer.prayerkmp.feature.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.feature.favorites.view_model.FavoritePrayerUi
import com.vladdrummer.prayerkmp.feature.favorites.view_model.FavoritesViewState
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    viewState: FavoritesViewState,
    onOpenPrayer: (FavoritePrayerUi) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onMoveFavorite: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    if (viewState.isLoading) {
        Text("Загрузка...")
        return
    }
    if (viewState.items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Список избранных молитв пока пуст. Чтоб добавить в него - заходите в молитвы и нажимайте на сердечко в верху экрана",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(listState) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                            offset.y.toInt() in info.offset..(info.offset + info.size)
                        }
                        draggingIndex = target?.index ?: -1
                        draggingOffsetY = 0f
                    },
                    onDragEnd = {
                        draggingIndex = -1
                        draggingOffsetY = 0f
                    },
                    onDragCancel = {
                        draggingIndex = -1
                        draggingOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        val currentIndex = draggingIndex
                        if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                        change.consume()
                        draggingOffsetY += dragAmount.y

                        val visible = listState.layoutInfo.visibleItemsInfo
                        val currentInfo = visible.firstOrNull { it.index == currentIndex } ?: return@detectDragGesturesAfterLongPress
                        val currentTop = currentInfo.offset + draggingOffsetY
                        val currentBottom = currentTop + currentInfo.size
                        val currentCenter = (currentTop + currentBottom) / 2f

                        val targetInfo = visible.firstOrNull { info ->
                            info.index != currentIndex && currentCenter.toInt() in info.offset..(info.offset + info.size)
                        }
                        if (targetInfo != null) {
                            onMoveFavorite(currentIndex, targetInfo.index)
                            draggingIndex = targetInfo.index
                            draggingOffsetY += (currentInfo.offset - targetInfo.offset)
                        }

                        val viewportStart = listState.layoutInfo.viewportStartOffset
                        val viewportEnd = listState.layoutInfo.viewportEndOffset
                        val overscrollTop = viewportStart - currentTop
                        val overscrollBottom = currentBottom - viewportEnd
                        when {
                            overscrollTop > 0f -> scope.launch {
                                listState.scrollBy(-overscrollTop.coerceAtMost(36f))
                            }
                            overscrollBottom > 0f -> scope.launch {
                                listState.scrollBy(overscrollBottom.coerceAtMost(36f))
                            }
                        }
                    }
                )
            },
        state = listState,
        contentPadding = PaddingValues(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(viewState.items, key = { _, item -> item.resId }) { index, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (index == draggingIndex) draggingOffsetY else 0f
                    }
                    .alpha(if (index == draggingIndex) 0.92f else 1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = draggingIndex < 0) { onOpenPrayer(item) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onRemoveFavorite(item.resId) }) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "Удалить из избранного",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
