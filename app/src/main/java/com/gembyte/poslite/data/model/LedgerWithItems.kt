package com.gembyte.poslite.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.gembyte.poslite.data.local.entity.CustomerLedgerEntity
import com.gembyte.poslite.data.local.entity.CustomerLedgerItemEntity

data class LedgerWithItems(

    @Embedded
    val ledger: CustomerLedgerEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "ledgerId"
    )
    val items: List<CustomerLedgerItemEntity>
)