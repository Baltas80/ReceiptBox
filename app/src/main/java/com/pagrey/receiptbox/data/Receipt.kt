package com.pagrey.receiptbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String = "",
    val date: String = "",
    val total: Double? = null,
    val tax: Double? = null,
    val receiptNumber: String = "",
    val category: String = "Otros",
    val imagePath: String = "",
    val rawText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
