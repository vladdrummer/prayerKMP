package org.example.prayerkmp.feature.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.example.prayerkmp.feature.mainmenu.MainMenuScreen
import org.example.prayerkmp.feature.mainmenu.view_model.MainViewModel

@Composable
fun PrayerNavigation (
    modifier: Modifier,
    navController: NavHostController
) {
    NavHost(
        modifier = modifier,
        startDestination = MainMenu,
        navController = navController
    ) {
        composable<MainMenu> {
            val viewModel : MainViewModel = viewModel { MainViewModel() }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            MainMenuScreen(
                viewState
            )
        }
    }
}