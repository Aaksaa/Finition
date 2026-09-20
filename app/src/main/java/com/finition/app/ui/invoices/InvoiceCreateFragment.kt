package com.finition.app.ui.invoices

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.finition.app.FinitionApp
import com.finition.app.R
import com.finition.app.data.LineItemDraft
import com.finition.app.data.model.InvoiceStatus
import com.finition.app.data.model.TransactionEntity
import com.finition.app.databinding.FragmentInvoiceCreateBinding
import com.finition.app.ui.common.CategoryLabels
import com.finition.app.util.Format
import java.util.Calendar
import kotlinx.coroutines.launch

class InvoiceCreateFragment : Fragment() {

    private var _binding: FragmentInvoiceCreateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvoicesViewModel by viewModels {
        viewModelFactory {
            initializer {
                InvoicesViewModel((requireActivity().application as FinitionApp).repository)
            }
        }
    }

    private var dueDate: Long = defaultDueDate()
    private val checkedIncomeIds = mutableSetOf<Long>()
    private var unbilled: List<TransactionEntity> = emptyList()
    private val manualItems = mutableListOf<LineItemDraft>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInvoiceCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateDueDateLabel()
        binding.dueDateButton.setOnClickListener { showDueDatePicker() }

        binding.addManualItemButton.setOnClickListener { addManualItem() }

        binding.saveButton.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.unbilledIncome.collect { list ->
                    unbilled = list
                    renderUnbilledIncome()
                }
            }
        }
    }

    // ---------- Unbilled income (auto-invoice source) ----------

    private fun renderUnbilledIncome() {
        val container = binding.unbilledContainer
        container.removeAllViews()

        if (unbilled.isEmpty()) {
            binding.unbilledEmpty.visibility = View.VISIBLE
            updateTotals()
            return
        }
        binding.unbilledEmpty.visibility = View.GONE

        unbilled.forEach { transaction ->
            val row = CheckBox(requireContext()).apply {
                text = getString(
                    R.string.invoice_unbilled_row,
                    incomeDescription(transaction),
                    Format.money(transaction.amountCents),
                )
                isChecked = checkedIncomeIds.contains(transaction.id)
                setTextColor(requireContext().getColor(R.color.on_dark_mute))
                textSize = 15f
                minHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) checkedIncomeIds.add(transaction.id)
                    else checkedIncomeIds.remove(transaction.id)
                    updateTotals()
                }
            }
            container.addView(row)
        }
        updateTotals()
    }

    private fun incomeDescription(transaction: TransactionEntity): String =
        transaction.notes.ifBlank {
            "${CategoryLabels.label(requireContext(), transaction.category)} — ${Format.date(transaction.date)}"
        }

    // ---------- Manual items ----------

    private fun addManualItem() {
        val description = binding.manualDescriptionInput.text.toString().trim()
        val amountCents = Format.parseAmountToCents(binding.manualAmountInput.text.toString())
        when {
            description.isEmpty() ->
                binding.manualDescriptionInput.error = getString(R.string.error_description_required)
            amountCents == null || amountCents <= 0 ->
                binding.manualAmountInput.error = getString(R.string.error_amount_required)
            else -> {
                manualItems.add(LineItemDraft(description, amountCents))
                binding.manualDescriptionInput.text?.clear()
                binding.manualAmountInput.text?.clear()
                renderManualItems()
            }
        }
    }

    private fun renderManualItems() {
        val container = binding.manualItemsContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        manualItems.forEachIndexed { index, item ->
            val row = inflater.inflate(R.layout.item_manual_line, container, false)
            row.findViewById<TextView>(R.id.item_description).text = item.description
            row.findViewById<TextView>(R.id.item_amount).text = Format.money(item.amountCents)
            row.findViewById<View>(R.id.item_remove).setOnClickListener {
                manualItems.removeAt(index)
                renderManualItems()
            }
            container.addView(row)
        }
        updateTotals()
    }

    // ---------- Totals ----------

    private fun currentDrafts(): List<LineItemDraft> {
        val fromIncome = unbilled
            .filter { checkedIncomeIds.contains(it.id) }
            .map { LineItemDraft(incomeDescription(it), it.amountCents, it.id) }
        return fromIncome + manualItems
    }

    private fun taxPercent(): Double =
        binding.taxInput.text.toString().trim().toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0

    private fun updateTotals() {
        val subtotal = currentDrafts().sumOf { it.amountCents }
        val tax = (subtotal * taxPercent() / 100.0).toLong()
        binding.subtotalLabel.text = Format.money(subtotal)
        binding.taxLabel.text = Format.money(tax)
        binding.totalLabel.text = Format.money(subtotal + tax)
    }

    // ---------- Date ----------

    private fun defaultDueDate(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 14)
        return cal.timeInMillis
    }

    private fun showDueDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day, 23, 59, 59)
                dueDate = cal.timeInMillis
                updateDueDateLabel()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun updateDueDateLabel() {
        binding.dueDateButton.text = Format.date(dueDate)
    }

    // ---------- Save ----------

    private fun save() {
        val clientName = binding.clientNameInput.text.toString().trim()
        if (clientName.isEmpty()) {
            binding.clientNameInput.error = getString(R.string.error_client_required)
            return
        }
        val items = currentDrafts()
        if (items.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.error_line_items_required,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val invoiceId = viewModel.createInvoice(
                clientName = clientName,
                clientEmail = binding.clientEmailInput.text.toString().trim(),
                issueDate = System.currentTimeMillis(),
                dueDate = dueDate,
                taxPercent = taxPercent(),
                notes = binding.notesInput.text.toString().trim(),
                lineItems = items,
                status = InvoiceStatus.DRAFT,
            )
            val args = Bundle().apply { putLong("invoiceId", invoiceId) }
            findNavController().navigate(R.id.invoiceDetailFragment, args)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
