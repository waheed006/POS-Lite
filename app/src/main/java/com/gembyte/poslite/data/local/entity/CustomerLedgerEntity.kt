package com.gembyte.poslite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gembyte.poslite.data.model.LedgerType

@Entity(tableName = "customer_ledger")
data class CustomerLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val date: Long = System.currentTimeMillis(),
    val type: LedgerType,
    val amount: Double,
    val note: String = ""
)