package com.planora.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.data.Graph
import com.planora.app.notify.NotificationHelper
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.OpenRequest
import com.planora.app.ui.PlanoraRoot
import com.planora.app.ui.theme.PlanoraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_TYPE = "open_type"
        const val EXTRA_OPEN_ID = "open_id"
    }

    private val vm: AppViewModel by viewModels()
    private val openRequests = MutableStateFlow<OpenRequest?>(null)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.rescheduleReminders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Graph.init(this)
        // Start-Bildschirm einmalig lesen, damit die Navigation nicht neu aufgebaut wird
        val startRoute = runBlocking { Graph.settings.current().startScreen }
        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= 33 && !NotificationHelper.canPost(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            PlanoraTheme(settings) {
                PlanoraRoot(vm, startRoute, openRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val type = intent?.getStringExtra(EXTRA_OPEN_TYPE) ?: return
        val id = intent.getLongExtra(EXTRA_OPEN_ID, 0)
        openRequests.value = OpenRequest(type, id)
        intent.removeExtra(EXTRA_OPEN_TYPE)
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
