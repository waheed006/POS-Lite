package com.gembyte.poslite.data.local.dao

import androidx.room.*
import com.gembyte.poslite.data.local.entity.BillWithItems
import com.gembyte.poslite.data.local.entity.SaleBillEntity
import com.gembyte.poslite.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    @Insert
    suspend fun insertBill(
        bill: SaleBillEntity
    ): Long

    @Insert
    suspend fun insertItems(
        items: List<SaleItemEntity>
    )

    @Transaction
    @Query("SELECT * FROM sale_bills ORDER BY billDate DESC")
    fun getBills(): Flow<List<BillWithItems>>

    @Delete
    suspend fun deleteBill(
        bill: SaleBillEntity
    )

    @Delete
    suspend fun deleteItems(
        items: List<SaleItemEntity>
    )

    @Query("SELECT * FROM sale_bills")
    suspend fun getAllBills(): List<SaleBillEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItems(): List<SaleItemEntity>

    @Query("DELETE FROM sale_bills")
    suspend fun deleteAllBills()

    @Query("DELETE FROM sale_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM sale_items WHERE billId=:billId")
    suspend fun getItemsByBillId(billId: Long): List<SaleItemEntity>

    @Query("DELETE FROM sale_items WHERE id=:itemId")
    suspend fun deleteSaleItem(itemId: Long)

    @Update
    suspend fun updateBill(bill: SaleBillEntity)

    @Transaction
    suspend fun updateBillAfterItemRemoval(
        bill: BillWithItems,
        removedItems: List<SaleItemEntity>,
        productDao: ProductDao
    ) {

        removedItems.forEach { item ->

            val product = productDao.getById(item.productId)
            product?.let {

                productDao.update(
                    it.copy(quantity = it.quantity + item.quantity)
                )
            }

            deleteSaleItem(item.id)
        }

        val remaining = getItemsByBillId(bill.bill.id)

        if (remaining.isEmpty()) {
            deleteBill(bill.bill)
            return
        }

        val newSale =
            remaining.sumOf {
                (it.wholesalePrice - it.discount) * it.quantity
            }

        val newProfit =
            remaining.sumOf {

                (
                        (
                                it.wholesalePrice -
                                        it.discount
                                ) -
                                it.purchasePrice
                        ) *
                        it.quantity
            } -
                    bill.bill.overallDiscount

        updateBill(
            bill.bill.copy(
                totalAmount =
                    maxOf(
                        0.0,
                        newSale -
                                bill.bill.overallDiscount
                    ),

                totalProfit = newProfit
            )
        )
    }
}