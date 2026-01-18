package org.example.prayerkmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.example.prayerkmp.feature.navigation.PrayerNavigation
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
             //   .safeContentPadding()
                .fillMaxSize()
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars
            ) { padding ->
                PrayerNavigation(
                    modifier = Modifier.padding(paddingValues = padding),
                    navController = navController
                )
            }
        }
    }
}