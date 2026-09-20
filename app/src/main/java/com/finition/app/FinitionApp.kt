package com.finition.app

import android.app.Application
import com.finition.app.data.FinanceRepository
import com.finition.app.data.db.FinitionDatabase
import com.finition.app.notifications.NotificationHelper

class FinitionApp : Application() {

    val repository: FinanceRepository by lazy {
        FinanceRepository(FinitionDatabase.get(this))
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
