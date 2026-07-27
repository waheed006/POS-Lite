package com.gembyte.poslite.ui.navigation

import kotlinx.serialization.Serializable

sealed class AppDestination {

    @Serializable
    data object Splash : AppDestination()

    @Serializable
    data object Pin : AppDestination()

    @Serializable
    data object Home : AppDestination()

    @Serializable
    data object Products : AppDestination()

    @Serializable
    data object Reports : AppDestination()

    @Serializable
    data object CreditSales : AppDestination()

    @Serializable
    data object Customers : AppDestination()
}