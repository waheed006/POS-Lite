package com.gembyte.poslite.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gembyte.poslite.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {

    @Insert
    suspend fun insert(company: CompanyEntity): Long

    @Update
    suspend fun update(company: CompanyEntity)

    @Delete
    suspend fun delete(company: CompanyEntity)

    @Query("""
        SELECT *
        FROM companies
        ORDER BY name
    """)
    fun getCompanies(): Flow<List<CompanyEntity>>

}