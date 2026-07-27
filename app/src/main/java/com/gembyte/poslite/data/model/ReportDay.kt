package com.gembyte.poslite.data.model

import com.gembyte.poslite.data.local.entity.BillWithItems

data class ReportDay(
    val date: String,
    val totalSale: Double,
    val totalProfit: Double,
    val bills: List<BillWithItems>
)