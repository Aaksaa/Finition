package com.finition.app.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class TransactionType { INCOME, EXPENSE }

enum class TransactionCategory(val type: TransactionType) {
    SALES(TransactionType.INCOME),
    SERVICES(TransactionType.INCOME),
    CONSULTING(TransactionType.INCOME),
    OTHER_INCOME(TransactionType.INCOME),
    RENT(TransactionType.EXPENSE),
    UTILITIES(TransactionType.EXPENSE),
    SUPPLIES(TransactionType.EXPENSE),
    SOFTWARE(TransactionType.EXPENSE),
    TRAVEL(TransactionType.EXPENSE),
    MARKETING(TransactionType.EXPENSE),
    PAYROLL(TransactionType.EXPENSE),
    OTHER_EXPENSE(TransactionType.EXPENSE);

    companion object {
        fun forType(type: TransactionType): List<TransactionCategory> =
            entries.filter { it.type == type }
    }
}

enum class InvoiceStatus { DRAFT, SENT, PAID }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val category: TransactionCategory,
    /** Amount in cents to avoid floating point errors. */
    val amountCents: Long,
    /** Timestamp in epoch millis. */
    val date: Long,
    val notes: String = "",
    val clientName: String? = null,
    /** Set when this income entry has been billed on an invoice. */
    val invoiceId: Long? = null,
)

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["invoiceNumber"], unique = true)],
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val clientName: String,
    val clientEmail: String = "",
    val issueDate: Long,
    val dueDate: Long,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    /** Tax percentage, e.g. 8.5 means 8.5%. */
    val taxPercent: Double = 0.0,
    val notes: String = "",
)

@Entity(
    tableName = "invoice_line_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("invoiceId")],
)
data class InvoiceLineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val description: String,
    /** Line total in cents. */
    val amountCents: Long,
    /** Source income transaction, when the line was auto-generated from the ledger. */
    val transactionId: Long? = null,
)

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val items: List<InvoiceLineItemEntity>,
) {
    val subtotalCents: Long get() = items.sumOf { it.amountCents }
    val taxCents: Long get() = (subtotalCents * invoice.taxPercent / 100.0).toLong()
    val totalCents: Long get() = subtotalCents + taxCents

    fun isOverdue(now: Long): Boolean =
        invoice.status == InvoiceStatus.SENT && invoice.dueDate < now
}

/** Invoice row joined with its computed line-item subtotal for list displays. */
data class InvoiceSummary(
    @Embedded val invoice: InvoiceEntity,
    val totalCents: Long,
)

/** Per-category expense aggregate. */
data class CategoryAmount(
    val category: TransactionCategory,
    val total: Long,
)

/** Per-month, per-type aggregate straight from SQL. */
data class MonthTypeTotal(
    val year: Int,
    val month: Int,
    val type: TransactionType,
    val total: Long,
)
