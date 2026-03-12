package com.vladdrummer.prayerkmp.feature.mainmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vladdrummer.prayerkmp.getPlatform
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.MainViewState
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.MainMenuItem
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainMenuScreen(
    onNavigateToContentListStarted: (String) -> Unit = {},
    viewState: MainViewState,
    onItemClick: (MainMenuItem) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var isNavigationInProgress by remember { mutableStateOf(false) }
    val platformName = remember { getPlatform().name }
    val useLiquid = remember { isLiquidSupportedOnCurrentDevice(platformName) }
    val liquidState = if (useLiquid) rememberLiquidState() else null

    val menuBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
            MaterialTheme.colorScheme.surface
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ImageBackground(
            liquidState = liquidState,
            useLiquid = useLiquid,
            backgroundBrush = menuBackground,
            modifier = Modifier.fillMaxSize()
        )

        val minCardWidth = calculateMinCardWidth(maxWidth, viewState.items)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCardWidth),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
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
                    isGlass = useLiquid && liquidState != null,
                    liquidState = liquidState,
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

@Composable
private fun ImageBackground(
    liquidState: LiquidState?,
    useLiquid: Boolean,
    backgroundBrush: Brush,
    modifier: Modifier = Modifier
) {
    if (useLiquid && liquidState != null) {
        Box(
            modifier = modifier
                .liquefiable(liquidState)
                .background(backgroundBrush)
        )
    } else {
        Box(
            modifier = modifier.background(backgroundBrush)
        )
    }
}

private fun isLiquidSupportedOnCurrentDevice(platformName: String): Boolean {
    if (!platformName.startsWith("Android ")) return false
    val apiLevel = platformName.removePrefix("Android ").trim().toIntOrNull() ?: return false
    return apiLevel >= 33
}

private val WordDelimiterRegex = Regex("""[\s\-]+""")

@OptIn(ExperimentalTextApi::class)
@Composable
private fun calculateMinCardWidth(
    width: Dp,
    items: List<MainMenuItem>,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.titleSmall.copy(lineHeight = 17.sp)

    val longestWord = items
        .asSequence()
        .flatMap { it.title.split(WordDelimiterRegex).asSequence() }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .maxByOrNull { it.length }
        ?: ""

    val longestWordWidthDp = with(density) {
        textMeasurer
            .measure(
                text = AnnotatedString(longestWord),
                style = textStyle,
                softWrap = false
            )
            .size
            .width
            .toDp()
    }

    val horizontalInsets = (12.dp * 2) + 4.dp
    val minWidth = (longestWordWidthDp + horizontalInsets).coerceAtLeast(136.dp)
    val upperBound = (width - 8.dp).coerceAtLeast(150.dp)
    return minWidth.coerceAtMost(upperBound)
}
