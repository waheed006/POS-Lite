package com.gembyte.poslite.ui.screens.product.editProductDetail

data class EditableProduct(
    val id: Long,

    var productName: String,
    var urduName: String,

    var purchasePrice: String,
    var wholesalePrice: String,

    var barcode: String,

    val companyId: Long
)