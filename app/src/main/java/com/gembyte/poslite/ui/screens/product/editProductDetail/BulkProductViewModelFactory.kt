package com.gembyte.poslite.ui.screens.product.editProductDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gembyte.poslite.data.local.dao.CompanyDao
import com.gembyte.poslite.data.local.dao.ProductDao

class BulkProductViewModelFactory(
    private val productDao: ProductDao,
    private val companyDao: CompanyDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(BulkProductViewModel::class.java)) {

            return BulkProductViewModel(
                productDao = productDao,
                companyDao = companyDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}