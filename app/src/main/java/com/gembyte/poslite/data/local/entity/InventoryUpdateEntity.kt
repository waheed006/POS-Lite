package com.gembyte.poslite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "inventory_updates")
data class InventoryUpdateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val date: Long = System.currentTimeMillis(),
    val oldQuantity: Int,
    val addedQuantity: Int,
    val newQuantity: Int,
    val oldPurchasePrice: Double,
    val enteredPurchasePrice: Double?,
    val finalPurchasePrice: Double,
    val note: String = ""
)