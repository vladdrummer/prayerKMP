package com.vladdrummer.prayerkmp.feature.psalter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.feature.prayer.PlatformPrayerText
import com.vladdrummer.prayerkmp.feature.psalter.view_model.PsalterReaderViewState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PsalterReaderScreen(
    viewState: PsalterReaderViewState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
    onOpenKathisma: (Int) -> Unit = {},
    currentKathisma: Int = 1,
    onFinishReading: () -> Unit = {},
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

    if (viewState.pages.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Нет данных для отображения")
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = viewState.currentPage.coerceIn(0, viewState.pages.lastIndex),
        pageCount = { viewState.pages.size }
    )
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        val pageData = viewState.pages[page]
        val scope = rememberCoroutineScope()
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = pageData.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFAA2C2C),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                PlatformPrayerText(
                    html = pageData.html,
                    fontSizeSp = 20,
                    fontIndex = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!pageData.isAfterKathismaPrayer && page < viewState.pages.lastIndex) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Листайте на следующую страницу")
                    }
                }
                if (pageData.isAfterKathismaPrayer) {
                    val nextKathisma = currentKathisma + 1
                    if (nextKathisma <= 20) {
                        Button(
                            onClick = { onOpenKathisma(nextKathisma) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("К Кафизме $nextKathisma")
                        }
                    }
                    Button(
                        onClick = onFinishReading,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("К молитве по окончанию чтения Псалтири")
                    }
                }
            }
        }
    }
}
