package com.gembyte.poslite.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart

val drawerItems = listOf(

    DrawerScreen(
        title = "Home",
        route = AppDestination.Home,
        icon = Icons.Default.Home
    ),

    DrawerScreen(
        title = "Products",
        route = AppDestination.Products,
        icon = Icons.Default.ShoppingCart
    ),

    DrawerScreen(
        title = "Reports",
        route = AppDestination.Reports,
        icon = Icons.Default.Assessment
    ),

    DrawerScreen(
        title = "Company",
        route = AppDestination.Companies,
        icon = Icons.Default.Factory
    ),

    DrawerScreen(
        title = "Customers",
        route = AppDestination.Customers,
        icon = Icons.Default.People
    )
)