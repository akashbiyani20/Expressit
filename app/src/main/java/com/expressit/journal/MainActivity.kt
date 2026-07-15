package com.expressit.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expressit.journal.ui.entry.EntryScreen
import com.expressit.journal.ui.entry.EntryViewModel
import com.expressit.journal.ui.home.HomeScreen
import com.expressit.journal.ui.home.HomeViewModel
import com.expressit.journal.ui.theme.ExpressItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressItTheme {
                ExpressItNavHost()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val ENTRY = "entry/{epochDay}?entryId={entryId}"

    fun entry(epochDay: Long, entryId: Long = -1L) = "entry/$epochDay?entryId=$entryId"
}

@Composable
private fun ExpressItNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(
            route = Routes.HOME,
            enterTransition = { fadeIn(tween(240)) },
            exitTransition = { fadeOut(tween(180)) }
        ) {
            HomeScreen(
                viewModel = viewModel(factory = HomeViewModel.Factory),
                onNewEntry = { date ->
                    navController.navigate(Routes.entry(date.toEpochDay()))
                },
                onOpenEntry = { date, entryId ->
                    navController.navigate(Routes.entry(date.toEpochDay(), entryId))
                }
            )
        }

        composable(
            route = Routes.ENTRY,
            arguments = listOf(
                navArgument("epochDay") { type = NavType.LongType },
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            // The entry screen rises like turning to a fresh page.
            enterTransition = {
                slideInVertically(tween(320)) { it / 4 } + fadeIn(tween(320))
            },
            exitTransition = { fadeOut(tween(160)) },
            popExitTransition = {
                slideOutVertically(tween(260)) { it / 4 } + fadeOut(tween(260))
            }
        ) {
            EntryScreen(
                viewModel = viewModel(factory = EntryViewModel.Factory),
                onClose = { navController.popBackStack() }
            )
        }
    }
}
