package com.finition.app.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finition.app.R
import com.finition.app.data.model.TransactionEntity
import com.finition.app.data.model.TransactionType
import com.finition.app.databinding.ItemTransactionBinding
import com.finition.app.ui.common.CategoryLabels
import com.finition.app.util.Format

class TransactionAdapter(
    private val onClick: (TransactionEntity) -> Unit,
) : ListAdapter<TransactionEntity, TransactionAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionEntity) {
            val context = binding.root.context
            binding.categoryLabel.text = CategoryLabels.label(context, item.category)
            binding.dateLabel.text = Format.date(item.date)
            binding.notesLabel.text = item.notes
            binding.notesLabel.visibility =
                if (item.notes.isBlank()) View.GONE else View.VISIBLE

            val isIncome = item.type == TransactionType.INCOME
            binding.amountLabel.text = Format.moneySigned(
                if (isIncome) item.amountCents else -item.amountCents,
            )
            binding.amountLabel.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isIncome) R.color.badge_success else R.color.primary,
                ),
            )

            binding.invoicedBadge.visibility =
                if (item.invoiceId != null) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TransactionEntity>() {
            override fun areItemsTheSame(a: TransactionEntity, b: TransactionEntity) = a.id == b.id
            override fun areContentsTheSame(a: TransactionEntity, b: TransactionEntity) = a == b
        }
    }
}
