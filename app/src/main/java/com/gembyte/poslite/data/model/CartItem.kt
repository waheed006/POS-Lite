package com.gembyte.poslite.data.model

import com.gembyte.poslite.data.local.entity.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
)