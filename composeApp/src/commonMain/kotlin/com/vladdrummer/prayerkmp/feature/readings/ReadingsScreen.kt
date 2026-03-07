package com.vladdrummer.prayerkmp.feature.readings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.feature.readings.view_model.ReadingsViewState
import com.vladdrummer.prayerkmp.feature.prayer.PrayerTextComposable

@Composable
fun ReadingsScreen(
    viewState: ReadingsViewState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    LaunchedEffect(viewState.isLoading, viewState.errorText, viewState.htmlText) {
        println(
            "readings: screen state loading=${viewState.isLoading}, " +
                "error=${viewState.errorText ?: "null"}, htmlChars=${viewState.htmlText.length}"
        )
    }

    if (viewState.isLoading) {
        println("readings: screen render loading")
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
        println("readings: screen render error=${viewState.errorText}")
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
                onClick = {
                    println("readings: screen retry clicked")
                    onRetry()
                },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Повторить")
            }
        }
        return
    }

    println("readings: screen render content htmlChars=${viewState.htmlText.length}")
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PrayerTextComposable(
            html = viewState.htmlText,
            fontSizeSp = 20,
            fontIndex = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
