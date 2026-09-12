package com.pagrey.receiptbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Receipt::class], version = 2, exportSchema = false)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE receipts ADD COLUMN tax REAL")
                database.execSQL("ALTER TABLE receipts ADD COLUMN category TEXT NOT NULL DEFAULT 'Otros'")
                database.execSQL("ALTER TABLE receipts ADD COLUMN imagePath TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE receipts ADD COLUMN rawText TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE receipts ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var INSTANCE: ReceiptDatabase? = null

        fun getInstance(context: Context): ReceiptDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDatabase::class.java,
                    "receiptbox.db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
