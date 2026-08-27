package com.gembyte.poslite.data.local.db

import androidx.room.Database
import com.gembyte.poslite.data.local.dao.ProductDao
import com.gembyte.poslite.data.local.entity.ProductEntity
import com.gembyte.poslite.data.local.dao.SalesDao
import com.gembyte.poslite.data.local.entity.SaleItemEntity

import androidx.room.RoomDatabase

import androidx.room.TypeConverters
import com.gembyte.poslite.data.local.converter.RoomConverters
import com.gembyte.poslite.data.local.dao.CompanyDao
import com.gembyte.poslite.data.local.dao.CustomerDao
import com.gembyte.poslite.data.local.dao.InventoryUpdateDao
import com.gembyte.poslite.data.local.entity.CompanyEntity
import com.gembyte.poslite.data.local.entity.CustomerEntity
import com.gembyte.poslite.data.local.entity.InventoryUpdateEntity
import com.gembyte.poslite.data.local.entity.SaleBillEntity

@Database(
    entities = [
        ProductEntity::class,
        SaleBillEntity::class,
        SaleItemEntity::class,
        CustomerEntity::class,
        InventoryUpdateEntity::class,
        CompanyEntity::class
    ],
    version = 6
)

@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun salesDao(): SalesDao
    abstract fun customerDao(): CustomerDao
    abstract fun inventoryUpdateDao(): InventoryUpdateDao
    abstract fun companyDao(): CompanyDao
}