package com.gembyte.poslite.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BillWithItems(

    @Embedded
    val bill: SaleBillEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val items: List<SaleItemEntity>
)