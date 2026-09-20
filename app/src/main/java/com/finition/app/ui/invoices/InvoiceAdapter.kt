package com.finition.app.ui.invoices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finition.app.data.model.InvoiceStatus
import com.finition.app.data.model.InvoiceSummary
import com.finition.app.databinding.ItemInvoiceBinding
import com.finition.app.ui.common.StatusBadges
import com.finition.app.util.Format

class InvoiceAdapter(
    private val onClick: (InvoiceSummary) -> Unit,
) : ListAdapter<InvoiceSummary, InvoiceAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemInvoiceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InvoiceSummary) {
            val invoice = item.invoice
            binding.invoiceNumber.text = invoice.invoiceNumber
            binding.clientName.text = invoice.clientName
            binding.dateLabel.text =
                "${Format.date(invoice.issueDate)}  →  ${Format.date(invoice.dueDate)}"
            binding.totalLabel.text = Format.money(item.totalCents)

            val isOverdue =
                invoice.status == InvoiceStatus.SENT && invoice.dueDate < Format.startOfToday()
            StatusBadges.apply(binding.statusBadge, invoice.status, isOverdue)

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<InvoiceSummary>() {
            override fun areItemsTheSame(a: InvoiceSummary, b: InvoiceSummary) =
                a.invoice.id == b.invoice.id

            override fun areContentsTheSame(a: InvoiceSummary, b: InvoiceSummary) = a == b
        }
    }
}
