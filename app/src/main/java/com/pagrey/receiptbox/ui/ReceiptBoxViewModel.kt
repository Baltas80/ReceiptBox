package com.pagrey.receiptbox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pagrey.receiptbox.data.Receipt
import com.pagrey.receiptbox.data.ReceiptDatabase
import com.pagrey.receiptbox.data.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReceiptBoxViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReceiptRepository(ReceiptDatabase.getInstance(application).receiptDao())

    val receipts = repository.observeAll()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(value: String) { _query.value = value }

    fun delete(receipt: Receipt) = viewModelScope.launch { repository.delete(receipt) }

    fun save(receipt: Receipt, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        if (receipt.id == 0L) {
            // Room's auto-generated id is the authoritative insertion order. This avoids
            // confusing the app's sequence with a number printed by the shop on the receipt.
            val id = repository.insert(receipt.copy(receiptNumber = ""))
            repository.getById(id)?.let { saved ->
                repository.update(saved.copy(receiptNumber = id.toString()))
            }
            onSaved(id)
        } else {
            repository.update(receipt)
            onSaved(receipt.id)
        }
    }

    fun restore(receipts: List<Receipt>, onRestored: (Int) -> Unit = {}) = viewModelScope.launch {
        if (receipts.isNotEmpty()) repository.insertAll(receipts.map { it.copy(id = 0L, receiptNumber = "") })
        onRestored(receipts.size)
    }

    fun restoreFull(receipts: List<Receipt>, onRestored: (Int) -> Unit = {}) = viewModelScope.launch {
        if (receipts.isNotEmpty()) repository.insertAll(receipts.map { it.copy(id = 0L, receiptNumber = "") })
        onRestored(receipts.size)
    }

    fun get(id: Long, onResult: (Receipt?) -> Unit) = viewModelScope.launch {
        onResult(repository.getById(id))
    }
}
