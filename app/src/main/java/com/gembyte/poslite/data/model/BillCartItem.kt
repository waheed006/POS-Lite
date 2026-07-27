package com.gembyte.poslite.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gembyte.poslite.data.local.entity.ProductEntity

class BillCartItem(
    val product: ProductEntity
) {
    var quantity by mutableIntStateOf(1)
    // discount PER UNIT
    var discount by mutableDoubleStateOf(0.0)
    var showDiscount by mutableStateOf(false)
}