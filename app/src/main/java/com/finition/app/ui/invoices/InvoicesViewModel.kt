package com.finition.app.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finition.app.data.FinanceRepository
import com.finition.app.data.LineItemDraft
import com.finition.app.data.model.InvoiceStatus
import com.finition.app.data.model.InvoiceSummary
import com.finition.app.data.model.InvoiceWithItems
import com.finition.app.data.model.TransactionEntity
import com.finition.app.util.Format
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class InvoiceFilter { ALL, DRAFT, SENT, PAID, OVERDUE }

class InvoicesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val now = MutableStateFlow(Format.startOfToday())
    val filter = MutableStateFlow(InvoiceFilter.ALL)

    val invoices: StateFlow<List<InvoiceSummary>> =
        combine(repository.observeInvoiceSummaries(), filter, now) { list, f, nowMillis ->
            when (f) {
                InvoiceFilter.ALL -> list
                InvoiceFilter.OVERDUE -> list.filter {
                    it.invoice.status == InvoiceStatus.SENT && it.invoice.dueDate < nowMillis
                }
                InvoiceFilter.DRAFT -> list.filter { it.invoice.status == InvoiceStatus.DRAFT }
                InvoiceFilter.SENT -> list.filter {
                    it.invoice.status == InvoiceStatus.SENT && it.invoice.dueDate >= nowMillis
                }
                InvoiceFilter.PAID -> list.filter { it.invoice.status == InvoiceStatus.PAID }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unbilledIncome: StateFlow<List<TransactionEntity>> =
        repository.observeUnbilledIncome()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeInvoice(invoiceId: Long): Flow<InvoiceWithItems?> =
        repository.observeInvoice(invoiceId)

    suspend fun createInvoice(
        clientName: String,
        clientEmail: String,
        issueDate: Long,
        dueDate: Long,
        taxPercent: Double,
        notes: String,
        lineItems: List<LineItemDraft>,
        status: InvoiceStatus,
    ): Long = repository.createInvoice(
        clientName, clientEmail, issueDate, dueDate, taxPercent, notes, lineItems, status,
    )

    suspend fun markSent(invoiceId: Long) =
        repository.updateInvoiceStatus(invoiceId, InvoiceStatus.SENT)

    suspend fun markPaid(invoiceId: Long) =
        repository.updateInvoiceStatus(invoiceId, InvoiceStatus.PAID)

    suspend fun deleteInvoice(invoiceId: Long) = repository.deleteInvoice(invoiceId)
}
