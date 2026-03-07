package com.vladdrummer.prayerkmp.feature.mainmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
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
        val columns = calculateColumns(maxWidth)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
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
                        .aspectRatio(132f / 156f)
                        .padding(horizontal = 2.dp)
                )
            }
        }
    }
}

private fun calculateColumns(width: Dp): Int {
    return when {
        width < 360.dp -> 2
        width < 600.dp -> 3
        else -> 4
    }
}
