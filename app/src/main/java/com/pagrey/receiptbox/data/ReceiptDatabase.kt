package com.pagrey.receiptbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Receipt::class], version = 1, exportSchema = false)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile private var INSTANCE: ReceiptDatabase? = null

        fun getInstance(context: Context): ReceiptDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDatabase::class.java,
                    "receiptbox.db"
                ).build().also { INSTANCE = it }
            }
    }
}
