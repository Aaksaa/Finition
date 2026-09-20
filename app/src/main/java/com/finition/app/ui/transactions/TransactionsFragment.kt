package com.finition.app.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.finition.app.FinitionApp
import com.finition.app.R
import com.finition.app.databinding.FragmentTransactionsBinding
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels {
        viewModelFactory {
            initializer {
                TransactionsViewModel((requireActivity().application as FinitionApp).repository)
            }
        }
    }

    private val adapter = TransactionAdapter { transaction ->
        val args = Bundle().apply { putLong("transactionId", transaction.id) }
        findNavController().navigate(R.id.action_transactions_to_edit, args)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.transactionList.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionList.adapter = adapter
        binding.transactionList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(requireContext().getDrawable(R.drawable.divider_hairline)!!)
            },
        )

        binding.filterGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.filter.value = when (checkedId) {
                R.id.filter_income -> TransactionFilter.INCOME
                R.id.filter_expense -> TransactionFilter.EXPENSE
                else -> TransactionFilter.ALL
            }
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_transactions_to_edit)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transactions.collect { list ->
                    adapter.submitList(list)
                    binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
