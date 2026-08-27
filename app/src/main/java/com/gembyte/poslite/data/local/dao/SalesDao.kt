package com.gembyte.poslite.data.local.dao

import androidx.room.*
import com.gembyte.poslite.data.local.entity.BillWithItems
import com.gembyte.poslite.data.local.entity.SaleBillEntity
import com.gembyte.poslite.data.local.entity.SaleItemEntity
import com.gembyte.poslite.data.model.DailyProductSale
import com.gembyte.poslite.data.model.DailySaleAnalytics
import com.gembyte.poslite.data.model.PeriodSalesSummary
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

    @Query(
        """
    SELECT
    COALESCE(SUM(quantity),0)
    FROM sale_items
    WHERE productId=:productId
    """
    )
    fun getUnitsSold(productId: Long): Flow<Int>

    @Query("""
    SELECT
    COALESCE(
    SUM((quantity * wholesalePrice)-(quantity * discount)),0)
    FROM sale_items
    WHERE productId=:productId
    """)
    fun getRevenue(productId: Long): Flow<Double>

    @Query("""
    SELECT
    COALESCE(
    SUM(((wholesalePrice-discount)-purchasePrice)* quantity),0)
    FROM sale_items
    WHERE productId=:productId
    """)
    fun getProfit(productId: Long): Flow<Double>

    @Query("""
    SELECT COALESCE(SUM(quantity),0)
    FROM sale_items
    WHERE productId=:productId
    """)
    fun getTotalSold(
        productId: Long
    ): Flow<Int>

    @Query("""
    SELECT COUNT(DISTINCT billId)
    FROM sale_items
    WHERE productId=:productId
    """)
    fun getBillCount(productId: Long): Flow<Int>

    @Query("""
    SELECT MAX(billDate)
    FROM sale_bills
    INNER JOIN sale_items
    ON sale_bills.id=sale_items.billId
    WHERE productId=:productId
    """)
    fun getLastSale(productId: Long): Flow<Long?>

    @Query(
        """
    SELECT 
        (sale_bills.billDate / 86400000) AS day,
        COALESCE(SUM(sale_items.quantity), 0) AS quantity,
        COALESCE(
            SUM(
                (sale_items.quantity * sale_items.wholesalePrice)
                -
                (sale_items.quantity * sale_items.discount)
            ),
            0
        ) AS revenue
    FROM sale_items
    INNER JOIN sale_bills
        ON sale_bills.id = sale_items.billId
    WHERE sale_items.productId = :productId
        AND sale_bills.billDate >= :startDate
        AND sale_bills.billDate < :endDate
    GROUP BY day
    ORDER BY day
    """
    )
    suspend fun getProductDailySales(
        productId: Long,
        startDate: Long,
        endDate: Long
    ): List<DailyProductSale>

    @Query("""
    SELECT
        date(sale_bills.billDate / 1000, 'unixepoch', 'localtime') AS date,

        COALESCE(
            SUM(sale_items.quantity),
            0
        ) AS unitsSold,

        COALESCE(
            SUM(
                (
                    sale_items.wholesalePrice -
                    sale_items.discount
                ) * sale_items.quantity
            ),
            0
        ) AS revenue,

        COALESCE(
            SUM(
                (
                    (
                        sale_items.wholesalePrice -
                        sale_items.discount
                    ) -
                    sale_items.purchasePrice
                ) * sale_items.quantity
            ),
            0
        ) AS profit

    FROM sale_items

    INNER JOIN sale_bills
        ON sale_bills.id = sale_items.billId

    WHERE
        sale_items.productId = :productId

        AND sale_bills.billDate >= :startDate

        AND sale_bills.billDate < :endDate

    GROUP BY
        date(
            sale_bills.billDate / 1000,
            'unixepoch',
            'localtime'
        )

    ORDER BY
        sale_bills.billDate ASC
""")
    suspend fun getDailySales(
        productId: Long,
        startDate: Long,
        endDate: Long
    ): List<DailySaleAnalytics>

    @Query("""
    SELECT
        COALESCE(SUM(sale_items.quantity), 0) AS unitsSold,

        COALESCE(
            SUM(
                (
                    sale_items.wholesalePrice -
                    sale_items.discount
                ) * sale_items.quantity
            ),
            0
        ) AS revenue,

        COALESCE(
            SUM(
                (
                    (
                        sale_items.wholesalePrice -
                        sale_items.discount
                    ) -
                    sale_items.purchasePrice
                ) * sale_items.quantity
            ),
            0
        ) AS profit

    FROM sale_items

    INNER JOIN sale_bills
        ON sale_bills.id = sale_items.billId

    WHERE
        sale_items.productId = :productId

        AND sale_bills.billDate >= :startDate

        AND sale_bills.billDate < :endDate
""")
    suspend fun getPeriodSalesSummary(
        productId: Long,
        startDate: Long,
        endDate: Long
    ): PeriodSalesSummary
}