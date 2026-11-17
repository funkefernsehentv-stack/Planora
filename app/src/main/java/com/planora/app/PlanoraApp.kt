package com.planora.app

import android.app.Application
import com.planora.app.data.Graph
import com.planora.app.notify.MaintenanceWorker
import com.planora.app.notify.NotificationHelper
import com.planora.app.notify.ReminderScheduler
import kotlinx.coroutines.launch

class PlanoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        MaintenanceWorker.schedule(this)
        Graph.appScope.launch {
            runCatching { NotificationHelper.ensureChannels(this@PlanoraApp, Graph.settings.current()) }
            runCatching { ReminderScheduler.rescheduleAll(this@PlanoraApp) }
        }
    }
}
