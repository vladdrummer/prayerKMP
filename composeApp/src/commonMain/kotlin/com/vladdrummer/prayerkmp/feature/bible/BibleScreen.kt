package com.vladdrummer.prayerkmp.feature.bible

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.feature.bible.view_model.BibleMode
import com.vladdrummer.prayerkmp.feature.bible.view_model.BibleViewState
import com.vladdrummer.prayerkmp.feature.prayer.PrayerTextComposable
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.ic_reading_nav_font_aa
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BibleScreen(
    viewState: BibleViewState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onBookClick: (String) -> Unit = {},
    onChapterClick: (String, Int) -> Unit = { _, _ -> },
    lastReadBookOverride: String? = null,
    lastReadChapterOverride: Int? = null,
    onContinueReadClick: (String, Int) -> Unit = { _, _ -> },
    onOpenReaderAt: (String, Int) -> Unit = { _, _ -> },
    onReaderPageChanged: (Int) -> Unit = {},
    onIncreaseFont: () -> Unit = {},
    onDecreaseFont: () -> Unit = {},
    onSwitchFont: () -> Unit = {},
    onResetFontDefaults: () -> Unit = {},
) {
    if (viewState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (viewState.errorText != null) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = viewState.errorText,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Повторить")
            }
        }
        return
    }

    when (viewState.mode) {
        BibleMode.Books -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val lastBook = lastReadBookOverride ?: viewState.lastReadBook
                val lastChapter = lastReadChapterOverride ?: viewState.lastReadChapter
                if (lastBook != null && lastChapter != null) {
                    item {
                        Card(
                            onClick = { onContinueReadClick(lastBook, lastChapter) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Продолжить чтение",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = "$lastBook, $lastChapter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Оглавление",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(viewState.books) { bookName ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            onClick = { onBookClick(bookName) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = bookName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                        if (viewState.expandedBook == bookName) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                viewState.chapters.forEach { chapter ->
                                    Card(
                                        onClick = { onChapterClick(bookName, chapter) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text(
                                            text = chapter.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .width(64.dp)
                                                .padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        BibleMode.Reader -> {
            val chapters = viewState.chapters
            val currentChapter = chapters.getOrNull(viewState.currentReaderPage)
            val currentBook = viewState.selectedBook
            var controlsExpanded by remember { mutableStateOf(false) }
            val fabRotation = animateFloatAsState(
                targetValue = if (controlsExpanded) 45f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "bible_fab_rotation"
            )
            val controlIconsTint = MaterialTheme.colorScheme.onPrimaryContainer

            Box(modifier = modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (currentBook != null && currentChapter != null) {
                            "$currentBook, глава $currentChapter"
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFAA2C2C),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                    key(viewState.readerSessionId) {
                        val pagerState = rememberPagerState(
                            initialPage = viewState.currentReaderPage,
                            pageCount = { chapters.size }
                        )
                        val scope = rememberCoroutineScope()
                        LaunchedEffect(pagerState.currentPage) {
                            onReaderPageChanged(pagerState.currentPage)
                        }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val chapterNumber = chapters.getOrNull(page)
                            val chapterHtml = chapterNumber?.let { viewState.chapterTexts[it] }.orEmpty()
                            if (chapterNumber == null || viewState.loadingChapters.contains(chapterNumber) && chapterHtml.isBlank()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                val scrollState = rememberScrollState()
                                val hasNextChapter = page < chapters.lastIndex
                                val nextBookName = if (!hasNextChapter) {
                                    val selectedBook = viewState.selectedBook
                                    val idx = viewState.books.indexOf(selectedBook)
                                    if (idx >= 0 && idx < viewState.books.lastIndex) viewState.books[idx + 1] else null
                                } else {
                                    null
                                }
                                val bottomButtonText = when {
                                    hasNextChapter -> "листайте к следующей главе"
                                    nextBookName != null -> "Перейти к следующей книге, $nextBookName"
                                    else -> null
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PrayerTextComposable(
                                        html = chapterHtml,
                                        fontSizeSp = viewState.fontSizeSp,
                                        fontIndex = viewState.fontIndex,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (bottomButtonText != null) {
                                        Button(
                                            onClick = {
                                                if (hasNextChapter) {
                                                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                                                } else if (nextBookName != null) {
                                                    onOpenReaderAt(nextBookName, 1)
                                                }
                                            },
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .widthIn(min = 220.dp)
                                        ) {
                                            Text(
                                                text = bottomButtonText,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = controlsExpanded,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                slideInHorizontally(
                                    initialOffsetX = { it / 3 },
                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                ) +
                                expandHorizontally(
                                    expandFrom = Alignment.End,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ),
                        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutHorizontally(
                                    targetOffsetX = { it / 3 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                ) +
                                shrinkHorizontally(
                                    shrinkTowards = Alignment.End,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                    ) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 2.dp,
                            shadowElevation = 4.dp,
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Шрифт / язык",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Row(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = onIncreaseFont, modifier = Modifier.size(42.dp)) {
                                        Icon(
                                            imageVector = Icons.Outlined.TextIncrease,
                                            contentDescription = "Шрифт больше",
                                            tint = controlIconsTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    IconButton(onClick = onDecreaseFont, modifier = Modifier.size(42.dp)) {
                                        Icon(
                                            imageVector = Icons.Outlined.TextDecrease,
                                            contentDescription = "Шрифт меньше",
                                            tint = controlIconsTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    IconButton(onClick = onSwitchFont, modifier = Modifier.size(42.dp)) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_reading_nav_font_aa),
                                            contentDescription = "Сменить шрифт",
                                            tint = controlIconsTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    IconButton(onClick = onResetFontDefaults, modifier = Modifier.size(42.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.RestartAlt,
                                            contentDescription = "Вернуть к настройкам по умолчанию",
                                            tint = controlIconsTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = { controlsExpanded = !controlsExpanded },
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Параметры текста",
                            modifier = Modifier
                                .size(30.dp)
                                .rotate(fabRotation.value)
                        )
                    }
                }
            }
        }
    }
}
