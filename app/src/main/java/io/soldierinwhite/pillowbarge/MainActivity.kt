package io.soldierinwhite.pillowbarge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.soldierinwhite.pillowbarge.home.Home
import io.soldierinwhite.pillowbarge.addstory.AddStory
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PillowBargeTheme {
                val navController = rememberNavController()
                window.statusBarColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        Home(windowWidthSizeClass = calculateWindowSizeClass(activity = this@MainActivity).widthSizeClass) {
                            navController.navigate("addStory")
                        }
                    }
                    composable(
                        route = "addStory",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                    ) {
                        AddStory {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}
