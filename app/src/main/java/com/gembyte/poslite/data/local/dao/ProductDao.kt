package com.gembyte.poslite.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gembyte.poslite.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY productName")
    fun getProducts(): Flow<List<ProductEntity>>

    @Insert
    suspend fun insert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id=:id")
    suspend fun getProduct(id: Long): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query(" SELECT * FROM products WHERE id=:id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Query(
        """
    SELECT *
    FROM products
    WHERE companyId=:companyId
    ORDER BY productName
    """
    )
    fun getProductsByCompany(
        companyId: Long
    ): Flow<List<ProductEntity>>
}