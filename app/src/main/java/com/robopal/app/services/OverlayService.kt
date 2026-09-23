package com.robopal.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.AgentState
import com.robopal.app.ui.robot.RobotFace
import com.robopal.app.ui.theme.RoboPalTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1003
        var instance: OverlayService? = null
    }

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var idleTimerJob: Job? = null

    private val serviceViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (Settings.canDrawOverlays(this)) {
            showOverlayWindow()
            observeAgentStateForEphemeralFace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayWindow()
        idleTimerJob?.cancel()
        serviceViewModelStore.clear()
        if (instance == this) {
            instance = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    fun showFace() {
        composeView?.visibility = View.VISIBLE
    }

    fun hideFace() {
        composeView?.visibility = View.GONE
    }

    private fun observeAgentStateForEphemeralFace() {
        lifecycleScope.launch {
            RoboPalApplication.agentEngine.state.collect { state ->
                idleTimerJob?.cancel()
                when (state) {
                    AgentState.LISTENING,
                    AgentState.THINKING,
                    AgentState.SPEAKING,
                    AgentState.WORKING,
                    AgentState.ERROR -> {
                        showFace()
                    }
                    AgentState.IDLE -> {
                        idleTimerJob = lifecycleScope.launch {
                            delay(5000)
                            if (RoboPalApplication.agentEngine.state.value == AgentState.IDLE) {
                                hideFace()
                                RoboPalApplication.agentEngine.clearHistory()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showOverlayWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            220,
            220,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            visibility = View.GONE // Oculto por defecto en modo efímero

            setContent {
                RoboPalTheme {
                    val agentState by RoboPalApplication.agentEngine.state.collectAsState(initial = AgentState.IDLE)
                    RobotFace(agentState = agentState)
                }
            }
        }

        windowManager?.addView(composeView, params)
    }

    private fun removeOverlayWindow() {
        if (composeView != null && windowManager != null) {
            windowManager?.removeView(composeView)
            composeView = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio Flotante RoboPal",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la cara flotante de RoboPal disponible en segundo plano."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("RoboPal - Flotante Activo")
            .setContentText("Cara flotante efímera lista en segundo plano")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
