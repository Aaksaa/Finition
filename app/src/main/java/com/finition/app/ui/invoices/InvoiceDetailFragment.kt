package com.finition.app.ui.invoices

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import com.finition.app.data.model.InvoiceStatus
import com.finition.app.data.model.InvoiceWithItems
import com.finition.app.databinding.FragmentInvoiceDetailBinding
import com.finition.app.ui.common.StatusBadges
import com.finition.app.util.Format
import kotlinx.coroutines.launch

class InvoiceDetailFragment : Fragment() {

    private var _binding: FragmentInvoiceDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvoicesViewModel by viewModels {
        viewModelFactory {
            initializer {
                InvoicesViewModel((requireActivity().application as FinitionApp).repository)
            }
        }
    }

    private val invoiceId: Long by lazy { arguments?.getLong("invoiceId") ?: -1L }
    private var current: InvoiceWithItems? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInvoiceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.markSentButton.setOnClickListener { setStatus(InvoiceStatus.SENT) }
        binding.markPaidButton.setOnClickListener { setStatus(InvoiceStatus.PAID) }
        binding.emailButton.setOnClickListener { sendByEmail() }
        binding.deleteButton.setOnClickListener { confirmDelete() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeInvoice(invoiceId).collect { invoice ->
                    if (invoice == null) {
                        findNavController().navigateUp()
                    } else {
                        current = invoice
                        render(invoice)
                    }
                }
            }
        }
    }

    private fun render(data: InvoiceWithItems) {
        val invoice = data.invoice

        binding.invoiceNumber.text = invoice.invoiceNumber
        val overdue = data.isOverdue(Format.startOfToday())
        StatusBadges.apply(binding.statusBadge, invoice.status, overdue)

        binding.clientName.text = invoice.clientName
        binding.clientEmail.text = invoice.clientEmail
        binding.clientEmail.visibility =
            if (invoice.clientEmail.isBlank()) View.GONE else View.VISIBLE

        binding.issueDate.text =
            getString(R.string.invoice_detail_issued_due, Format.date(invoice.issueDate), Format.date(invoice.dueDate))

        val container = binding.lineItemsContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        data.items.forEach { item ->
            val row = inflater.inflate(R.layout.item_invoice_line_display, container, false)
            row.findViewById<TextView>(R.id.item_description).text = item.description
            row.findViewById<TextView>(R.id.item_amount).text = Format.money(item.amountCents)
            container.addView(row)
        }

        binding.subtotalLabel.text = Format.money(data.subtotalCents)
        binding.taxLabel.text =
            "${Format.money(data.taxCents)} (${invoice.taxPercent}%)"
        binding.totalLabel.text = Format.money(data.totalCents)

        binding.notesLabel.text = invoice.notes
        binding.notesLabel.visibility =
            if (invoice.notes.isBlank()) View.GONE else View.VISIBLE

        binding.markSentButton.visibility =
            if (invoice.status == InvoiceStatus.DRAFT) View.VISIBLE else View.GONE
        binding.markPaidButton.visibility =
            if (invoice.status == InvoiceStatus.SENT) View.VISIBLE else View.GONE
    }

    private fun setStatus(status: InvoiceStatus) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.run { if (status == InvoiceStatus.SENT) markSent(invoiceId) else markPaid(invoiceId) }
        }
    }

    private fun sendByEmail() {
        val data = current ?: return
        val invoice = data.invoice

        val lines = buildString {
            appendLine("${invoice.invoiceNumber}")
            appendLine("${getString(R.string.invoice_bill_to)}: ${invoice.clientName}")
            appendLine(
                getString(
                    R.string.invoice_detail_issued_due,
                    Format.date(invoice.issueDate),
                    Format.date(invoice.dueDate),
                ),
            )
            appendLine()
            data.items.forEach {
                appendLine("${it.description} — ${Format.money(it.amountCents)}")
            }
            appendLine()
            appendLine("${getString(R.string.invoice_subtotal)}: ${Format.money(data.subtotalCents)}")
            appendLine("${getString(R.string.invoice_tax)} (${invoice.taxPercent}%): ${Format.money(data.taxCents)}")
            appendLine("${getString(R.string.invoice_total)}: ${Format.money(data.totalCents)}")
            if (invoice.notes.isNotBlank()) {
                appendLine()
                appendLine(invoice.notes)
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            if (invoice.clientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(invoice.clientEmail))
            }
            putExtra(
                Intent.EXTRA_SUBJECT,
                getString(R.string.email_invoice_subject, invoice.invoiceNumber, getString(R.string.app_name)),
            )
            putExtra(Intent.EXTRA_TEXT, lines)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.email_chooser_title)))
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.invoice_delete_confirm)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteInvoice(invoiceId)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
