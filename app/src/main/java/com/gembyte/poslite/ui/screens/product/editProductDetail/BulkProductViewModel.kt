package com.gembyte.poslite.ui.screens.product.editProductDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gembyte.poslite.data.local.dao.CompanyDao
import com.gembyte.poslite.data.local.dao.ProductDao
import com.gembyte.poslite.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BulkProductViewModel(
    private val productDao: ProductDao,
    private val companyDao: CompanyDao
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(BulkProductUiState())

    val uiState: StateFlow<BulkProductUiState> =
        _uiState.asStateFlow()

    val companies: StateFlow<List<CompanyEntity>> =
        companyDao
            .getCompanies()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    init {
        observeProducts()
    }

    private fun observeProducts() {

        viewModelScope.launch {

            productDao.getProducts()
                .collect { products ->

                    val editableProducts =
                        products.map { product ->

                            EditableProduct(
                                id = product.id,

                                productName =
                                    product.productName,

                                urduName =
                                    product.urduName,

                                purchasePrice =
                                    product.purchasePrice.toString(),

                                wholesalePrice =
                                    product.wholesalePrice.toString(),

                                barcode =
                                    product.barcode,

                                companyId =
                                    product.companyId
                            )
                        }

                    _uiState.update {
                        it.copy(
                            products = editableProducts,
                            isLoading = false
                        )
                    }
                }
        }
    }

    // ============================================================
    // SEARCH
    // ============================================================

    fun updateSearchQuery(
        query: String
    ) {

        _uiState.update {
            it.copy(
                searchQuery = query
            )
        }
    }

    // ============================================================
    // COMPANY FILTER
    // ============================================================

    fun selectCompany(
        companyId: Long?
    ) {

        _uiState.update {
            it.copy(
                selectedCompanyId = companyId
            )
        }
    }

    // ============================================================
    // INCOMPLETE FILTER
    // ============================================================

    fun toggleIncompleteFilter() {
        _uiState.update {
            it.copy(
                showIncompleteOnly = !it.showIncompleteOnly
            )
        }
    }

    // ============================================================
    // UPDATE PRODUCT FIELD
    // ============================================================

    fun updateProduct(
        productId: Long,
        update: (EditableProduct) -> EditableProduct
    ) {

        _uiState.update { state ->

            state.copy(
                products =
                    state.products.map { product ->

                        if (product.id == productId) {
                            update(product)
                        } else {
                            product
                        }
                    }
            )
        }
    }
}