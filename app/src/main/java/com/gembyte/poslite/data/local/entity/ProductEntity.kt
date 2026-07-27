package com.gembyte.poslite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gembyte.poslite.data.model.WeightUnit
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "products")
data class ProductEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val barcode: String,
    val purchasePrice: Double,
    val wholesalePrice: Double,
    val retailPrice: Double,
    val weightUnit: WeightUnit,
    val discount: Double = 0.0,
    val quantity: Int,
    val addDate: Long = System.currentTimeMillis(),
    val productImage: String? = null
)