package com.vladdrummer.prayerkmp.feature.mainmenu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.MainViewState
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.MainMenuItem
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.TodayHymnUi
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.TodayPreviewState
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.ic_main_holidays
import kotlinproject.composeapp.generated.resources.ic_main_meal
import kotlinproject.composeapp.generated.resources.ic_main_people
import kotlinproject.composeapp.generated.resources.ic_main_prayer
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainMenuScreen(
    onNavigateToContentListStarted: (String) -> Unit = {},
    viewState: MainViewState,
    onTodayClick: () -> Unit = {},
    onItemClick: (MainMenuItem) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var isNavigationInProgress by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                TodaySection(
                    todayPreview = viewState.todayPreview,
                    onOpenToday = onTodayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TodaySection(
    todayPreview: TodayPreviewState,
    onOpenToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headerColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(
        todayPreview.isLoading,
        todayPreview.errorText,
        todayPreview.references.size,
        todayPreview.celebrating,
        todayPreview.fast,
        todayPreview.nameDays,
        todayPreview.tropariAndKondaki.size
    ) {
        println(
            "today-preview-ui: render loading=${todayPreview.isLoading}, " +
                "error=${todayPreview.errorText ?: "null"}, refs=${todayPreview.references.size}, " +
                "celebrating='${todayPreview.celebrating.orEmpty().take(40)}', " +
                "fast='${todayPreview.fast.orEmpty().take(40)}', " +
                "nameDays='${todayPreview.nameDays.orEmpty().take(40)}', " +
                "hymns=${todayPreview.tropariAndKondaki.size}"
        )
    }
    if (todayPreview.isLoading) {
        Text(
            text = "Загрузка...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
        return
    }
    if (!todayPreview.errorText.isNullOrBlank()) {
        Text(
            text = todayPreview.errorText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        TodayItem(
            iconRes = Res.drawable.ic_main_prayer,
            title = "Открыть Евангельские чтения на сегодня",
            body = todayPreview.references.joinToString("; ").ifBlank { "—" },
            titleColor = headerColor,
            onClick = onOpenToday,
        )
        Spacer(modifier = Modifier.size(8.dp))
        TodayItem(
            iconRes = Res.drawable.ic_main_meal,
            title = "Пост сегодня",
            body = todayPreview.fast ?: "—",
            titleColor = headerColor,
        )
        Spacer(modifier = Modifier.size(8.dp))
        TodayItem(
            iconRes = Res.drawable.ic_main_people,
            title = "Сегодня именины празднуют",
            body = todayPreview.nameDays ?: "—",
            titleColor = headerColor,
        )
        Spacer(modifier = Modifier.size(8.dp))
        TodayItem(
            iconRes = Res.drawable.ic_main_holidays,
            title = "Сегодня праздник",
            body = todayPreview.celebrating ?: "—",
            titleColor = headerColor,
        )
        if (todayPreview.tropariAndKondaki.isNotEmpty()) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Тропари и кондаки дня",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = headerColor,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            todayPreview.tropariAndKondaki.forEachIndexed { index, hymn ->
                HymnItem(hymn = hymn, modifier = Modifier.padding(horizontal = 14.dp))
                if (index != todayPreview.tropariAndKondaki.lastIndex) {
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
private fun TodayItem(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    title: String,
    body: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .size(40.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HymnItem(
    hymn: TodayHymnUi,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = hymn.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!hymn.glas.isNullOrBlank()) {
                Text(
                    text = hymn.glas,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = hymn.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 1.35.em,
                    lineHeightStyle = LineHeightStyle.Default
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
