package com.finition.app.ui.common

import android.content.Context
import com.finition.app.R
import com.finition.app.data.model.TransactionCategory

object CategoryLabels {

    fun label(context: Context, category: TransactionCategory): String = context.getString(
        when (category) {
            TransactionCategory.SALES -> R.string.category_sales
            TransactionCategory.SERVICES -> R.string.category_services
            TransactionCategory.CONSULTING -> R.string.category_consulting
            TransactionCategory.OTHER_INCOME -> R.string.category_other_income
            TransactionCategory.RENT -> R.string.category_rent
            TransactionCategory.UTILITIES -> R.string.category_utilities
            TransactionCategory.SUPPLIES -> R.string.category_supplies
            TransactionCategory.SOFTWARE -> R.string.category_software
            TransactionCategory.TRAVEL -> R.string.category_travel
            TransactionCategory.MARKETING -> R.string.category_marketing
            TransactionCategory.PAYROLL -> R.string.category_payroll
            TransactionCategory.OTHER_EXPENSE -> R.string.category_other_expense
        },
    )
}
