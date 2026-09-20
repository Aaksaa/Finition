package com.finition.app.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.finition.app.FinitionApp
import com.finition.app.R
import com.finition.app.data.model.TransactionCategory
import com.finition.app.data.model.TransactionEntity
import com.finition.app.data.model.TransactionType
import com.finition.app.databinding.FragmentTransactionEditBinding
import com.finition.app.ui.common.CategoryLabels
import com.finition.app.util.Format
import java.util.Calendar
import kotlinx.coroutines.launch

class TransactionEditFragment : Fragment() {

    private var _binding: FragmentTransactionEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels {
        viewModelFactory {
            initializer {
                TransactionsViewModel((requireActivity().application as FinitionApp).repository)
            }
        }
    }

    private var editing: TransactionEntity? = null
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val transactionId = arguments?.getLong("transactionId", -1L) ?: -1L

        updateDateLabel()
        rebuildCategories(TransactionType.INCOME)

        binding.typeGroup.setOnCheckedChangeListener { _, checkedId ->
            rebuildCategories(
                if (checkedId == R.id.type_expense) TransactionType.EXPENSE else TransactionType.INCOME,
            )
        }

        binding.dateButton.setOnClickListener { showDatePicker() }

        binding.saveButton.setOnClickListener { save() }

        binding.deleteButton.setOnClickListener {
            val current = editing ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.delete(current)
                findNavController().navigateUp()
            }
        }

        if (transactionId > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getTransaction(transactionId)?.let { transaction ->
                    editing = transaction
                    prefill(transaction)
                }
            }
        }
    }

    private fun prefill(transaction: TransactionEntity) {
        binding.typeGroup.check(
            if (transaction.type == TransactionType.INCOME) R.id.type_income else R.id.type_expense,
        )
        binding.amountInput.setText("%.2f".format(transaction.amountCents / 100.0))
        selectedDate = transaction.date
        updateDateLabel()
        binding.notesInput.setText(transaction.notes)
        binding.clientInput.setText(transaction.clientName.orEmpty())
        binding.deleteButton.visibility = View.VISIBLE
        selectCategory(transaction.category)
    }

    private fun rebuildCategories(type: TransactionType) {
        val group = binding.categoryGroup
        group.removeAllViews()
        TransactionCategory.forType(type).forEachIndexed { index, category ->
            val pill = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = CategoryLabels.label(requireContext(), category)
                tag = category
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.space_sm)
                    topMargin = resources.getDimensionPixelSize(R.dimen.space_sm)
                }
            }
            pill.setTextAppearance(R.style.TextAppearance_Finition_MonoLabel)
            pill.setBackgroundResource(R.drawable.bg_tab_pill)
            pill.buttonDrawable = null
            pill.setTextColor(
                androidx.core.content.ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.selector_tab_text,
                ),
            )
            val padH = resources.getDimensionPixelSize(R.dimen.space_lg)
            val padV = resources.getDimensionPixelSize(R.dimen.space_sm)
            pill.setPadding(padH, padV, padH, padV)
            pill.minHeight = 0
            pill.minimumHeight = 0
            group.addView(pill)
            if (index == 0) group.check(pill.id)
        }
    }

    private fun selectedCategory(): TransactionCategory? {
        val checkedId = binding.categoryGroup.checkedRadioButtonId
        val pill = binding.categoryGroup.findViewById<RadioButton>(checkedId)
        return pill?.tag as? TransactionCategory
    }

    private fun selectCategory(category: TransactionCategory) {
        for (i in 0 until binding.categoryGroup.childCount) {
            val pill = binding.categoryGroup.getChildAt(i) as RadioButton
            if (pill.tag == category) {
                binding.categoryGroup.check(pill.id)
                return
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day, 12, 0, 0)
                selectedDate = cal.timeInMillis
                updateDateLabel()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun updateDateLabel() {
        binding.dateButton.text = Format.date(selectedDate)
    }

    private fun save() {
        val amountCents = Format.parseAmountToCents(binding.amountInput.text.toString())
        if (amountCents == null || amountCents <= 0) {
            binding.amountInput.error = getString(R.string.error_amount_required)
            return
        }
        val category = selectedCategory() ?: TransactionCategory.OTHER_INCOME
        val type =
            if (binding.typeGroup.checkedRadioButtonId == R.id.type_expense) {
                TransactionType.EXPENSE
            } else {
                TransactionType.INCOME
            }
        val client = binding.clientInput.text.toString().trim().ifEmpty { null }

        val existing = editing
        val transaction = TransactionEntity(
            id = existing?.id ?: 0,
            type = type,
            category = category,
            amountCents = amountCents,
            date = selectedDate,
            notes = binding.notesInput.text.toString().trim(),
            clientName = client,
            invoiceId = existing?.invoiceId,
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.save(transaction)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
