package com.finition.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finition.app.data.FinanceRepository
import com.finition.app.data.model.TransactionEntity
import com.finition.app.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class TransactionFilter { ALL, INCOME, EXPENSE }

class TransactionsViewModel(private val repository: FinanceRepository) : ViewModel() {

    val filter = MutableStateFlow(TransactionFilter.ALL)

    val transactions: StateFlow<List<TransactionEntity>> =
        combine(repository.observeTransactions(), filter) { list, f ->
            when (f) {
                TransactionFilter.ALL -> list
                TransactionFilter.INCOME -> list.filter { it.type == TransactionType.INCOME }
                TransactionFilter.EXPENSE -> list.filter { it.type == TransactionType.EXPENSE }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getTransaction(id: Long) = repository.getTransaction(id)

    suspend fun save(transaction: TransactionEntity) = repository.saveTransaction(transaction)

    suspend fun delete(transaction: TransactionEntity) = repository.deleteTransaction(transaction)
}
