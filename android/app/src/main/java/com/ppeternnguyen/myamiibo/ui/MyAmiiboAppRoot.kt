package com.ppeternnguyen.myamiibo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ppeternnguyen.myamiibo.ui.screens.BrowseScreen
import com.ppeternnguyen.myamiibo.ui.screens.CollectionScreen
import com.ppeternnguyen.myamiibo.ui.screens.DetailScreen

private object Routes {
    const val BROWSE_TAB = "browse-tab"
    const val COLLECTION_TAB = "collection-tab"
    const val BROWSE = "browse"
    const val COLLECTION = "collection"
    const val BROWSE_DETAIL = "browse/amiibo/{${DetailViewModel.ARG_ID}}"
    const val COLLECTION_DETAIL = "collection/amiibo/{${DetailViewModel.ARG_ID}}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.BROWSE_TAB, "Browse", Icons.Filled.Search),
    Tab(Routes.COLLECTION_TAB, "Collection", Icons.Filled.Favorite),
)

@Composable
fun MyAmiiboAppRoot() {
    val navController = rememberNavController()
    // One catalog ViewModel for the whole app, scoped to the activity.
    val catalogViewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.Factory)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val idArgument = listOf(navArgument(DetailViewModel.ARG_ID) { type = NavType.StringType })

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        // Each tab keeps its own back stack, so switching tabs never loses
        // your place in a detail page.
        NavHost(
            navController = navController,
            startDestination = Routes.BROWSE_TAB,
            modifier = Modifier.padding(padding),
        ) {
            navigation(startDestination = Routes.BROWSE, route = Routes.BROWSE_TAB) {
                composable(Routes.BROWSE) {
                    BrowseScreen(catalogViewModel, onOpenAmiibo = { id -> navController.navigate("browse/amiibo/$id") })
                }
                composable(Routes.BROWSE_DETAIL, arguments = idArgument) {
                    DetailScreen(catalogViewModel, onBack = { navController.popBackStack() })
                }
            }
            navigation(startDestination = Routes.COLLECTION, route = Routes.COLLECTION_TAB) {
                composable(Routes.COLLECTION) {
                    CollectionScreen(catalogViewModel, onOpenAmiibo = { id -> navController.navigate("collection/amiibo/$id") })
                }
                composable(Routes.COLLECTION_DETAIL, arguments = idArgument) {
                    DetailScreen(catalogViewModel, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
