package com.gembyte.poslite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gembyte.poslite.data.local.entity.InventoryUpdateEntity
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
}