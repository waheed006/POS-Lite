package com.gembyte.poslite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gembyte.poslite.data.local.entity.InventoryUpdateEntity
import com.gembyte.poslite.data.model.DailyStockAdded
import com.gembyte.poslite.data.model.DailyStockAnalytics
import com.gembyte.poslite.data.model.PeriodStockSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryUpdateDao {

    @Insert
    suspend fun insert(
        update: InventoryUpdateEntity
    )

    @Query("""
        SELECT *
        FROM inventory_updates
        WHERE productId=:productId
        ORDER BY date DESC
    """)
    fun getUpdates(
        productId: Long
    ): Flow<List<InventoryUpdateEntity>>

    @Query("""
        SELECT *
        FROM inventory_updates
        ORDER BY date DESC
    """)
    suspend fun getAll(): List<InventoryUpdateEntity>

    @Insert
    suspend fun insertAll(
        items: List<InventoryUpdateEntity>
    )

    @Query("DELETE FROM inventory_updates")
    suspend fun deleteAll()

    @Query("""
    SELECT
    COUNT(*)
    FROM inventory_updates
    WHERE productId=:productId
    """)
    fun getUpdateCount(productId: Long): Flow<Int>

    @Query(
        """
    SELECT
        (date / 86400000) AS day,
        COALESCE(SUM(addedQuantity), 0) AS quantity
    FROM inventory_updates
    WHERE productId = :productId
        AND date >= :startDate
        AND date < :endDate
    GROUP BY day
    ORDER BY day
    """
    )
    suspend fun getProductDailyStockAdded(
        productId: Long,
        startDate: Long,
        endDate: Long
    ): List<DailyStockAdded>

    @Query("""
    SELECT
        date(
            date / 1000,
            'unixepoch',
            'localtime'
        ) AS date,

        COALESCE(
            SUM(addedQuantity),
            0
        ) AS quantityAdded

    FROM inventory_updates

    WHERE
        productId = :productId

        AND date >= :startDate

        AND date < :endDate

    GROUP BY
        date(
            date / 1000,
            'unixepoch',
            'localtime'
        )

    ORDER BY
        date ASC
""")
    suspend fun getDailyStockAdded(
        productId: Long,
        startDate: Long,
        endDate: Long
    ): List<DailyStockAnalytics>

    @Query("""
    SELECT
        COALESCE(
            SUM(addedQuantity),
            0
        ) AS quantityAdded

    FROM inventory_updates

    WHERE
        productId = :productId

        AND date >= :startDate

        AND date < :endDate
""")
    suspend fun getPeriodStockSummary(
        productId: Long,
        startDate: Long,
        endDate: Long
    ): PeriodStockSummary
}