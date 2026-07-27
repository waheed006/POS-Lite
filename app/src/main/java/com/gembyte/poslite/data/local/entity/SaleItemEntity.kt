package com.gembyte.poslite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long,
    val productId: Long,
    val productName: String,
    val purchasePrice: Double,
    val wholesalePrice: Double,
    val quantity: Int,
    val discount: Double
)