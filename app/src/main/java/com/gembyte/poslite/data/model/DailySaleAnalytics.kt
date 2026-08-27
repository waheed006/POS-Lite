package com.gembyte.poslite.data.model

data class DailySaleAnalytics(
    val date: Long,
    val unitsSold: Int,
    val revenue: Double,
    val profit: Double
)

data class DailyStockAnalytics(
    val date: Long,
    val quantityAdded: Int
)

data class PeriodSalesSummary(
    val unitsSold: Int,
    val revenue: Double,
    val profit: Double
)

data class PeriodStockSummary(
    val quantityAdded: Int
)