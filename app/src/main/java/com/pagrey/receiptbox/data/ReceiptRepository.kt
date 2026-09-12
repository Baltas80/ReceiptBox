package com.pagrey.receiptbox.data

import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val dao: ReceiptDao) {
    fun observeAll(): Flow<List<Receipt>> = dao.observeAll()
    fun search(query: String): Flow<List<Receipt>> = dao.search(query)
    suspend fun getById(id: Long): Receipt? = dao.getById(id)
    suspend fun insert(receipt: Receipt): Long = dao.insert(receipt)
    suspend fun insertAll(receipts: List<Receipt>): List<Long> = dao.insertAll(receipts)
    suspend fun update(receipt: Receipt) = dao.update(receipt)
    suspend fun delete(receipt: Receipt) = dao.delete(receipt)
}
