package com.gembyte.poslite.ui.app

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gembyte.poslite.components.composables.AppDrawer
import com.gembyte.poslite.ui.navigation.AppDestination
import com.gembyte.poslite.ui.screens.home.HomeScreen
import com.gembyte.poslite.ui.screens.auth.PinScreen
import com.gembyte.poslite.ui.screens.auth.SplashScreen
import com.gembyte.poslite.ui.screens.credit.CreditScreen
import com.gembyte.poslite.ui.screens.customer.CustomerScreen
import com.gembyte.poslite.ui.screens.product.ProductScreen
import com.gembyte.poslite.ui.screens.reports.ReportScreen
import kotlinx.coroutines.launch

@Composable
fun App() {

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    val navController = rememberNavController()

    val currentBackStack by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        currentBackStack?.destination?.route

    AppDrawer(
        drawerState = drawerState,
        currentRoute = when (currentRoute) {

            AppDestination.Home::class.qualifiedName ->
                AppDestination.Home

            AppDestination.Products::class.qualifiedName ->
                AppDestination.Products

            AppDestination.Reports::class.qualifiedName ->
                AppDestination.Reports

            AppDestination.CreditSales::class.qualifiedName ->
                AppDestination.CreditSales

            else -> null
        },
        onItemClick = {
            navController.navigate(it) {
                launchSingleTop = true
                restoreState = true
                popUpTo(
                    navController.graph.startDestinationId
                ) {
                    saveState = true
                }
            }
            scope.launch { drawerState.close() }
        }
    ) {

        NavHost(
            navController = navController,

            startDestination =
                AppDestination.Splash
        ) {

            composable<AppDestination.Splash> {
                SplashScreen(
                    onNavigateToPin = {
                        navController.navigate(
                            AppDestination.Pin
                        ) {
                            popUpTo<AppDestination.Splash> {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<AppDestination.Pin> {
                PinScreen(
                    onLoginSuccess = {
                        navController.navigate(AppDestination.Home) {
                            popUpTo<AppDestination.Pin> {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<AppDestination.Home> {
                HomeScreen(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                )
            }

            composable<AppDestination.Products> {
                ProductScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AppDestination.Reports> {
                ReportScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AppDestination.CreditSales> {
                CreditScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AppDestination.Customers> {
                CustomerScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}