package com.finition.app.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.finition.app.R
import com.finition.app.data.MonthBucket
import com.finition.app.util.Format
import kotlin.math.max

/**
 * Grouped bar chart of monthly income vs expenses — colorimetric depth,
 * hairline baseline, mono month labels (Sanity system-readout aesthetic).
 */
class CashFlowChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var buckets: List<MonthBucket> = emptyList()

    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.badge_success)
    }
    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
    }
    private val baselinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.divider_dark)
        strokeWidth = 1f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ash)
        textSize = 11f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = runCatching { ResourcesCompat.getFont(context, R.font.ibm_plex_mono) }
            .getOrNull() ?: android.graphics.Typeface.MONOSPACE
    }

    private val barRect = RectF()

    fun setData(data: List<MonthBucket>) {
        buckets = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (buckets.isEmpty()) return

        val labelHeight = labelPaint.textSize * 2f
        val chartBottom = height - paddingBottom - labelHeight
        val chartTop = paddingTop.toFloat()
        val chartHeight = chartBottom - chartTop
        if (chartHeight <= 0) return

        val maxValue = buckets.maxOf { max(it.incomeCents, it.expenseCents) }.coerceAtLeast(1)

        canvas.drawLine(
            paddingLeft.toFloat(), chartBottom,
            (width - paddingRight).toFloat(), chartBottom,
            baselinePaint,
        )

        val groupWidth = (width - paddingLeft - paddingRight).toFloat() / buckets.size
        val barWidth = groupWidth * 0.22f
        val gap = groupWidth * 0.06f
        val cornerRadius = barWidth / 2f

        buckets.forEachIndexed { index, bucket ->
            val groupCenter = paddingLeft + groupWidth * index + groupWidth / 2f

            val incomeHeight = chartHeight * (bucket.incomeCents.toFloat() / maxValue)
            val expenseHeight = chartHeight * (bucket.expenseCents.toFloat() / maxValue)

            // Income bar (left of pair)
            if (bucket.incomeCents > 0) {
                barRect.set(
                    groupCenter - gap / 2f - barWidth,
                    chartBottom - incomeHeight,
                    groupCenter - gap / 2f,
                    chartBottom,
                )
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, incomePaint)
            }

            // Expense bar (right of pair)
            if (bucket.expenseCents > 0) {
                barRect.set(
                    groupCenter + gap / 2f,
                    chartBottom - expenseHeight,
                    groupCenter + gap / 2f + barWidth,
                    chartBottom,
                )
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, expensePaint)
            }

            canvas.drawText(
                Format.monthLabel(bucket.year, bucket.month).uppercase(),
                groupCenter,
                height - paddingBottom - labelPaint.textSize / 2f,
                labelPaint,
            )
        }
    }
}
