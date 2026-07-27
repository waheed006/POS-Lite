package com.gembyte.poslite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_ledger_items")
data class CustomerLedgerItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ledgerId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val salePrice: Double,
    val discount: Double
)