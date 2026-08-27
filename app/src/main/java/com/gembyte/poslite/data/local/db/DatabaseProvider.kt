package com.gembyte.poslite.data.local.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    private var database: AppDatabase? = null

    fun getDatabase(
        context: Context
    ): AppDatabase {

        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context, AppDatabase::class.java, "mini_pos.db"
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6
            ).build()

            database = instance
            instance
        }
    }
}