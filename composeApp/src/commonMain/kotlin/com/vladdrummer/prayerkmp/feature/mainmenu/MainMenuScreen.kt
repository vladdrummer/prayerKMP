package com.vladdrummer.prayerkmp.feature.mainmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.MainViewState
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.MainMenuItem
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainMenuScreen(
    onNavigateToContentListStarted: (String) -> Unit = {},
    viewState: MainViewState,
    onItemClick: (MainMenuItem) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var isNavigationInProgress by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        val minCardWidth = calculateMinCardWidth(maxWidth, viewState.items)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCardWidth),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(
                items = viewState.items,
                key = { _, item -> item.id }
            ) { _, item ->
                MainMenuCard(
                    item = MainMenuCardItem(
                        icon = painterResource(item.drawable),
                        text = item.title
                    ),
                    onClick = {
                        if (isNavigationInProgress) return@MainMenuCard
                        isNavigationInProgress = true
                        selectedItemId = item.id
                        onNavigateToContentListStarted(item.title)
                        onItemClick(item)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                )
            }
        }
    }
}

private fun calculateMinCardWidth(
    width: Dp,
    items: List<MainMenuItem>,
): Dp {
    val longestWordLength = items
        .asSequence()
        .flatMap { it.title.split(Regex("""[\s\-]+""")).asSequence() }
        .map { it.length }
        .maxOrNull() ?: 0

    var minWidth = when {
        width < 420.dp -> 170.dp
        width < 720.dp -> 180.dp
        else -> 190.dp
    }
    if (longestWordLength >= 14) minWidth += 14.dp
    if (longestWordLength >= 18) minWidth += 18.dp

    val upperBound = (width - 8.dp).coerceAtLeast(150.dp)
    return minWidth.coerceAtMost(upperBound)
}
