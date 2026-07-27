package com.gembyte.poslite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gembyte.poslite.data.model.PaymentType
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sale_bills")
data class SaleBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billDate: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val totalProfit: Double,
    val overallDiscount: Double = 0.0,
    val paymentType: PaymentType = PaymentType.CASH
)