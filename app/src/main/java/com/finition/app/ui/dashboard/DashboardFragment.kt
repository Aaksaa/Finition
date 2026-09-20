package com.finition.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.finition.app.databinding.FragmentDashboardBinding
import com.finition.app.ui.common.CategoryLabels
import com.finition.app.util.Format
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        viewModelFactory {
            initializer {
                DashboardViewModel((requireActivity().application as FinitionApp).repository)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.overdueBanner.setOnClickListener {
            findNavController().navigate(R.id.invoicesFragment)
        }
        binding.unbilledBanner.setOnClickListener {
            findNavController().navigate(R.id.invoicesFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.totalIncome.text = Format.money(state.totalIncomeCents)
                    binding.totalExpenses.text = Format.money(state.totalExpenseCents)
                    binding.netProfit.text = Format.moneySigned(state.netCents)
                    binding.netProfit.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (state.netCents >= 0) R.color.badge_success else R.color.primary,
                        ),
                    )

                    binding.cashFlowChart.setData(state.monthlyBuckets)

                    if (state.overdueCount > 0) {
                        binding.overdueBanner.visibility = View.VISIBLE
                        binding.overdueBannerText.text = getString(
                            R.string.dashboard_overdue_banner,
                            state.overdueCount,
                            Format.money(state.overdueOutstandingCents),
                        )
                    } else {
                        binding.overdueBanner.visibility = View.GONE
                    }

                    if (state.unbilledIncomeCents > 0) {
                        binding.unbilledBanner.visibility = View.VISIBLE
                        binding.unbilledBannerText.text =
                            getString(R.string.dashboard_unbilled_income, Format.money(state.unbilledIncomeCents))
                    } else {
                        binding.unbilledBanner.visibility = View.GONE
                    }

                    renderExpenseBreakdown(state)
                }
            }
        }
    }

    private fun renderExpenseBreakdown(state: com.finition.app.data.DashboardState) {
        val container = binding.expenseBreakdownContainer
        container.removeAllViews()

        if (state.expenseBreakdown.isEmpty()) {
            binding.expenseEmpty.visibility = View.VISIBLE
            return
        }
        binding.expenseEmpty.visibility = View.GONE

        val max = state.expenseBreakdown.maxOf { it.total }.coerceAtLeast(1)
        val inflater = LayoutInflater.from(requireContext())

        state.expenseBreakdown.forEach { item ->
            val row = inflater.inflate(R.layout.item_expense_breakdown, container, false)
            row.findViewById<TextView>(R.id.category_label).text =
                CategoryLabels.label(requireContext(), item.category)
            row.findViewById<TextView>(R.id.category_amount).text = Format.money(item.total)
            row.findViewById<ProgressBar>(R.id.category_progress).progress =
                ((item.total * 100) / max).toInt().coerceAtLeast(2)
            container.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
