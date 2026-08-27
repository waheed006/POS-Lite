package com.gembyte.poslite.data.model

data class ProductAnalytics(
    val totalSold: Int,
    val revenue: Double,
    val profit: Double,
    val currentStock: Int,
    val inventoryValue: Double,
    val totalStockAdded: Int,
    val stockUpdateCount: Int
)