package com.gembyte.poslite.data.model

import com.gembyte.poslite.data.local.dao.ProductDao
import com.gembyte.poslite.data.local.dao.SalesDao
import com.gembyte.poslite.data.local.entity.BillWithItems

suspend fun deleteSale(
    bill: BillWithItems,
    productDao: ProductDao,
    salesDao: SalesDao
) {

    bill.items.forEach { item ->

        val product =
            productDao.getProduct(
                item.productId
            )

        product?.let {

            productDao.update(
                it.copy(
                    quantity =
                        it.quantity + item.quantity
                )
            )
        }
    }

    salesDao.deleteItems(
        bill.items
    )

    salesDao.deleteBill(
        bill.bill
    )
}