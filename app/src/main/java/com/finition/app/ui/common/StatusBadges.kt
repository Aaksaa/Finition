package com.finition.app.ui.common

import android.content.Context
import android.widget.TextView
import com.finition.app.R
import com.finition.app.data.model.InvoiceStatus

object StatusBadges {

    /** Applies the Sanity badge treatment for an invoice status (with derived OVERDUE). */
    fun apply(view: TextView, status: InvoiceStatus, isOverdue: Boolean) {
        val context: Context = view.context
        when {
            isOverdue -> style(view, R.string.status_overdue, R.drawable.bg_badge_danger)
            status == InvoiceStatus.PAID -> style(view, R.string.status_paid, R.drawable.bg_badge_success)
            status == InvoiceStatus.SENT -> style(view, R.string.status_sent, R.drawable.bg_badge_info)
            else -> style(view, R.string.status_draft, R.drawable.bg_badge_muted)
        }
    }

    private fun style(view: TextView, textRes: Int, bgRes: Int) {
        view.setText(textRes)
        view.setBackgroundResource(bgRes)
        view.setTextColor(view.context.getColor(R.color.black))
    }
}
