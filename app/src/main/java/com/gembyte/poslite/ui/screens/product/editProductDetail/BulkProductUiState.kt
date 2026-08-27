package com.gembyte.poslite.ui.screens.product.editProductDetail

data class BulkProductUiState(
    val products: List<EditableProduct> = emptyList(),

    val searchQuery: String = "",

    val selectedCompanyId: Long? = null,

    val showIncompleteOnly: Boolean = false,

    val isLoading: Boolean = false,

    val isSaving: Boolean = false
)