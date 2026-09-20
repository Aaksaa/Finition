package com.finition.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finition.app.data.model.CategoryAmount
import com.finition.app.data.model.InvoiceEntity
import com.finition.app.data.model.InvoiceLineItemEntity
import com.finition.app.data.model.InvoiceStatus
import com.finition.app.data.model.InvoiceSummary
import com.finition.app.data.model.InvoiceWithItems
import com.finition.app.data.model.MonthTypeTotal
import com.finition.app.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'INCOME'")
    fun observeTotalIncomeCents(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'EXPENSE'")
    fun observeTotalExpenseCents(): Flow<Long>

    @Query(
        """
        SELECT CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) AS year,
               CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) AS month,
               type,
               SUM(amountCents) AS total
        FROM transactions
        GROUP BY year, month, type
        """
    )
    fun observeMonthlyTotals(): Flow<List<MonthTypeTotal>>

    @Query(
        """
        SELECT category, SUM(amountCents) AS total
        FROM transactions
        WHERE type = 'EXPENSE'
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeExpenseByCategory(): Flow<List<CategoryAmount>>

    /** Income entries not yet billed on any invoice. */
    @Query("SELECT * FROM transactions WHERE type = 'INCOME' AND invoiceId IS NULL ORDER BY date DESC")
    fun observeUnbilledIncome(): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'INCOME' AND invoiceId IS NULL")
    fun observeUnbilledIncomeCents(): Flow<Long>

    @Query("UPDATE transactions SET invoiceId = :invoiceId WHERE id IN (:transactionIds)")
    suspend fun markInvoiced(transactionIds: List<Long>, invoiceId: Long)

    @Query("UPDATE transactions SET invoiceId = NULL WHERE invoiceId = :invoiceId")
    suspend fun clearInvoiceLink(invoiceId: Long)
}

@Dao
interface InvoiceDao {

    @Insert
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert
    suspend fun insertLineItems(items: List<InvoiceLineItemEntity>)

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET status = :status WHERE id = :invoiceId")
    suspend fun updateStatus(invoiceId: Long, status: InvoiceStatus)

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteById(invoiceId: Long)

    @androidx.room.Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    fun observeInvoiceWithItems(invoiceId: Long): Flow<InvoiceWithItems?>

    @Query(
        """
        SELECT invoices.*,
               (SELECT COALESCE(SUM(amountCents), 0)
                  FROM invoice_line_items
                  WHERE invoice_line_items.invoiceId = invoices.id) AS totalCents
        FROM invoices
        ORDER BY issueDate DESC, id DESC
        """
    )
    fun observeInvoiceSummaries(): Flow<List<InvoiceSummary>>

    /** Sent invoices whose due date has passed — candidates for overdue reminders. */
    @Query(
        """
        SELECT invoices.*,
               (SELECT COALESCE(SUM(amountCents), 0)
                  FROM invoice_line_items
                  WHERE invoice_line_items.invoiceId = invoices.id) AS totalCents
        FROM invoices
        WHERE status = 'SENT' AND dueDate < :now
        ORDER BY dueDate ASC
        """
    )
    fun observeOverdueSummaries(now: Long): Flow<List<InvoiceSummary>>

    @Query("SELECT COALESCE(MAX(id), 0) FROM invoices")
    suspend fun maxInvoiceId(): Long
}
