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

val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {

        // Remove old credit tables
        db.execSQL("DROP TABLE IF EXISTS customer_ledger")
        db.execSQL("DROP TABLE IF EXISTS customer_ledger_items")

        // Create companies table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS companies(
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                image TEXT,
                description TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """
        )

        // Insert default company

        db.execSQL(
            """
            INSERT INTO companies(
                name,
                image,
                description,
                createdAt
            )
            VALUES(
                'Default',
                NULL,
                '',
                strftime('%s','now')*1000
            )
            """
        )

        // Add companyId into products
        db.execSQL(
            """
            ALTER TABLE products
            ADD COLUMN companyId INTEGER NOT NULL DEFAULT 1
            """
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {

    override fun migrate(
        db: SupportSQLiteDatabase
    ) {

        db.execSQL(
            """
            ALTER TABLE companies
            ADD COLUMN contactPerson TEXT NOT NULL DEFAULT ''
            """
        )

        db.execSQL(
            """
            ALTER TABLE companies
            ADD COLUMN phone TEXT NOT NULL DEFAULT ''
            """
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {

    override fun migrate(
        db: SupportSQLiteDatabase
    ) {

        db.execSQL(
            """
            ALTER TABLE products
            ADD COLUMN urduName TEXT NOT NULL DEFAULT ''
            """.trimIndent()
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {

    override fun migrate(
        db: SupportSQLiteDatabase
    ) {

        // Create the new simplified companies table
        db.execSQL(
            """
            CREATE TABLE companies_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                image TEXT
            )
            """.trimIndent()
        )

        // Copy existing company data
        db.execSQL(
            """
            INSERT INTO companies_new (
                id,
                name,
                description,
                image
            )
            SELECT
                id,
                name,
                description,
                image
            FROM companies
            """.trimIndent()
        )

        // Remove old table
        db.execSQL(
            "DROP TABLE companies"
        )

        // Rename new table
        db.execSQL(
            "ALTER TABLE companies_new RENAME TO companies"
        )
    }
}