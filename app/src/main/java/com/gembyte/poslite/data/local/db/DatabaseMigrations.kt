package com.gembyte.poslite.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(
        db: SupportSQLiteDatabase
    ) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS inventory_updates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                date INTEGER NOT NULL,
                oldQuantity INTEGER NOT NULL,
                addedQuantity INTEGER NOT NULL,
                newQuantity INTEGER NOT NULL,
                oldPurchasePrice REAL NOT NULL,
                enteredPurchasePrice REAL,
                finalPurchasePrice REAL NOT NULL,
                note TEXT NOT NULL
            )
            """
        )
    }
}