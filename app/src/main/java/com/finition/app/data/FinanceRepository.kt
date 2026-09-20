package com.finition.app.data

import androidx.room.withTransaction
import com.finition.app.data.db.FinitionDatabase
import com.finition.app.data.model.CategoryAmount
import com.finition.app.data.model.InvoiceEntity
import com.finition.app.data.model.InvoiceLineItemEntity
import com.finition.app.data.model.InvoiceStatus
import com.finition.app.data.model.InvoiceSummary
import com.finition.app.data.model.InvoiceWithItems
import com.finition.app.data.model.TransactionEntity
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/** One month of cash flow for the dashboard chart. */
data class MonthBucket(
    val year: Int,
    val month: Int, // 1-12
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
) {
    val netCents: Long get() = incomeCents - expenseCents
}

data class DashboardState(
    val totalIncomeCents: Long = 0,
    val totalExpenseCents: Long = 0,
    val netCents: Long = 0,
    val monthlyBuckets: List<MonthBucket> = emptyList(),
    val expenseBreakdown: List<CategoryAmount> = emptyList(),
    val overdueCount: Int = 0,
    val overdueOutstandingCents: Long = 0,
    val unbilledIncomeCents: Long = 0,
)

/** Draft of a line item before it is persisted. */
data class LineItemDraft(
    val description: String,
    val amountCents: Long,
    val transactionId: Long? = null,
)

class FinanceRepository(private val db: FinitionDatabase) {

    private val transactions = db.transactionDao()
    private val invoices = db.invoiceDao()

    // ---------- Dashboard ----------

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDashboard(now: Flow<Long>): Flow<DashboardState> {
        val overdue = now.flatMapLatest { invoices.observeOverdueSummaries(it) }
        return combine(
            transactions.observeTotalIncomeCents(),
            transactions.observeTotalExpenseCents(),
            transactions.observeMonthlyTotals(),
            transactions.observeExpenseByCategory(),
            overdue,
            transactions.observeUnbilledIncomeCents(),
        ) { values ->
            val income = values[0] as Long
            val expense = values[1] as Long
            @Suppress("UNCHECKED_CAST")
            val monthly = values[2] as List<com.finition.app.data.model.MonthTypeTotal>
            @Suppress("UNCHECKED_CAST")
            val breakdown = values[3] as List<CategoryAmount>
            @Suppress("UNCHECKED_CAST")
            val overdueList = values[4] as List<InvoiceSummary>
            val unbilled = values[5] as Long

            DashboardState(
                totalIncomeCents = income,
                totalExpenseCents = expense,
                netCents = income - expense,
                monthlyBuckets = buildBuckets(monthly),
                expenseBreakdown = breakdown,
                overdueCount = overdueList.size,
                overdueOutstandingCents = overdueList.sumOf { it.totalCents },
                unbilledIncomeCents = unbilled,
            )
        }
    }

    /** Last 6 calendar months (oldest first), zero-filled. */
    private fun buildBuckets(monthly: List<com.finition.app.data.model.MonthTypeTotal>): List<MonthBucket> {
        val cal = Calendar.getInstance()
        val keys = (5 downTo 0).map { back ->
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -back)
            c.get(Calendar.YEAR) to (c.get(Calendar.MONTH) + 1)
        }
        return keys.map { (year, month) ->
            MonthBucket(
                year = year,
                month = month,
                incomeCents = monthly.firstOrNull { it.year == year && it.month == month && it.type == com.finition.app.data.model.TransactionType.INCOME }?.total ?: 0,
                expenseCents = monthly.firstOrNull { it.year == year && it.month == month && it.type == com.finition.app.data.model.TransactionType.EXPENSE }?.total ?: 0,
            )
        }
    }

    // ---------- Transactions ----------

    fun observeTransactions(): Flow<List<TransactionEntity>> = transactions.observeAll()

    suspend fun getTransaction(id: Long): TransactionEntity? = transactions.getById(id)

    suspend fun saveTransaction(transaction: TransactionEntity): Long =
        if (transaction.id == 0L) transactions.insert(transaction)
        else {
            transactions.update(transaction)
            transaction.id
        }

    suspend fun deleteTransaction(transaction: TransactionEntity) = transactions.delete(transaction)

    fun observeUnbilledIncome(): Flow<List<TransactionEntity>> = transactions.observeUnbilledIncome()

    // ---------- Invoices ----------

    fun observeInvoiceSummaries(): Flow<List<InvoiceSummary>> = invoices.observeInvoiceSummaries()

    fun observeInvoice(invoiceId: Long): Flow<InvoiceWithItems?> =
        invoices.observeInvoiceWithItems(invoiceId)

    fun observeOverdueInvoices(now: Long): Flow<List<InvoiceSummary>> =
        invoices.observeOverdueSummaries(now)

    suspend fun nextInvoiceNumber(): String =
        "INV-%04d".format(invoices.maxInvoiceId() + 1)

    /**
     * Auto-invoicing: builds an invoice from drafts (optionally linked to income
     * transactions) and marks those transactions as billed in one DB transaction.
     */
    suspend fun createInvoice(
        clientName: String,
        clientEmail: String,
        issueDate: Long,
        dueDate: Long,
        taxPercent: Double,
        notes: String,
        lineItems: List<LineItemDraft>,
        status: InvoiceStatus = InvoiceStatus.DRAFT,
    ): Long = db.withTransaction {
        val invoiceId = invoices.insertInvoice(
            InvoiceEntity(
                invoiceNumber = nextInvoiceNumber(),
                clientName = clientName,
                clientEmail = clientEmail,
                issueDate = issueDate,
                dueDate = dueDate,
                status = status,
                taxPercent = taxPercent,
                notes = notes,
            ),
        )
        invoices.insertLineItems(
            lineItems.map {
                InvoiceLineItemEntity(
                    invoiceId = invoiceId,
                    description = it.description,
                    amountCents = it.amountCents,
                    transactionId = it.transactionId,
                )
            },
        )
        val linkedIds = lineItems.mapNotNull { it.transactionId }
        if (linkedIds.isNotEmpty()) {
            transactions.markInvoiced(linkedIds, invoiceId)
        }
        invoiceId
    }

    suspend fun updateInvoiceStatus(invoiceId: Long, status: InvoiceStatus) =
        invoices.updateStatus(invoiceId, status)

    /** Deletes the invoice and returns its linked transactions to the unbilled pool. */
    suspend fun deleteInvoice(invoiceId: Long) = db.withTransaction {
        transactions.clearInvoiceLink(invoiceId)
        invoices.deleteById(invoiceId)
    }
}
