package com.finition.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.finition.app.MainActivity
import com.finition.app.R
import com.finition.app.data.model.InvoiceSummary
import com.finition.app.util.Format

object NotificationHelper {

    private const val CHANNEL_ID = "invoice_reminders"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    /** Posts one reminder per overdue invoice (id-stable so repeats update, not stack). */
    fun notifyOverdue(context: Context, overdue: List<InvoiceSummary>) {
        if (overdue.isEmpty() || !canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val launchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        overdue.forEach { summary ->
            val invoice = summary.invoice
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_invoice)
                .setContentTitle(
                    context.getString(R.string.notification_overdue_title, invoice.invoiceNumber),
                )
                .setContentText(
                    context.getString(
                        R.string.notification_overdue_text,
                        invoice.clientName,
                        Format.money(summary.totalCents),
                        Format.date(invoice.dueDate),
                    ),
                )
                .setContentIntent(launchIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(invoice.id.toInt(), notification)
        }
    }
}
