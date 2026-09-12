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
                addColumnIfMissing(database, "tax", "REAL")
                addColumnIfMissing(database, "category", "TEXT NOT NULL DEFAULT 'Otros'")
                addColumnIfMissing(database, "imagePath", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(database, "rawText", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(database, "createdAt", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun addColumnIfMissing(
            database: SupportSQLiteDatabase,
            name: String,
            definition: String
        ) {
            database.query("PRAGMA table_info(receipts)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == name) return
                }
            }
            database.execSQL("ALTER TABLE receipts ADD COLUMN $name $definition")
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
