package com.finition.app.ui.invoices

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
import com.finition.app.databinding.FragmentInvoicesBinding
import kotlinx.coroutines.launch

class InvoicesFragment : Fragment() {

    private var _binding: FragmentInvoicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvoicesViewModel by viewModels {
        viewModelFactory {
            initializer {
                InvoicesViewModel((requireActivity().application as FinitionApp).repository)
            }
        }
    }

    private val adapter = InvoiceAdapter { summary ->
        val args = Bundle().apply { putLong("invoiceId", summary.invoice.id) }
        findNavController().navigate(R.id.action_invoices_to_detail, args)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInvoicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.invoiceList.layoutManager = LinearLayoutManager(requireContext())
        binding.invoiceList.adapter = adapter
        binding.invoiceList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(requireContext().getDrawable(R.drawable.divider_hairline)!!)
            },
        )

        binding.filterGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.filter.value = when (checkedId) {
                R.id.filter_draft -> InvoiceFilter.DRAFT
                R.id.filter_sent -> InvoiceFilter.SENT
                R.id.filter_paid -> InvoiceFilter.PAID
                R.id.filter_overdue -> InvoiceFilter.OVERDUE
                else -> InvoiceFilter.ALL
            }
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_invoices_to_create)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.invoices.collect { list ->
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
