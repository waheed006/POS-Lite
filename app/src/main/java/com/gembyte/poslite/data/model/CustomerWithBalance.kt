package com.gembyte.poslite.data.model

import androidx.room.Embedded
import com.gembyte.poslite.data.local.entity.CustomerEntity

data class CustomerWithBalance(
    @Embedded
    val customer: CustomerEntity,
    val outstanding: Double,
    val lastActivity: Long
)