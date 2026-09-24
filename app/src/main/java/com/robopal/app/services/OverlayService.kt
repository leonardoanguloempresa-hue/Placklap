package com.robopal.app.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.AgentState
import com.robopal.app.ui.robot.RobotFace
import com.robopal.app.ui.theme.RoboPalTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "OverlayService"
        var instance: OverlayService? = null
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val hideRunnable = Runnable {
        hideFaceWithAnimation()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        setupOverlay()
        observeAgentState()
    }

    private fun setupOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No hay permiso para overlay (SYSTEM_ALERT_WINDOW)")
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        val sizePx = (140 * density).toInt()

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (80 * density).toInt()
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            visibility = View.GONE
            scaleX = 0f
            scaleY = 0f

            setContent {
                RoboPalTheme {
                    val state by RoboPalApplication.agentEngine.state.collectAsState()
                    RobotFace(agentState = state)
                }
            }
        }

        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error añadiendo vista overlay: ${e.message}", e)
        }
    }

    private fun observeAgentState() {
        serviceScope.launch {
            RoboPalApplication.agentEngine.state.collectLatest { state ->
                when (state) {
                    AgentState.LISTENING, AgentState.THINKING, AgentState.WORKING, AgentState.SPEAKING -> {
                        handler.removeCallbacks(hideRunnable)
                        showFaceWithAnimation()
                    }
                    AgentState.IDLE -> {
                        handler.removeCallbacks(hideRunnable)
                        handler.postDelayed(hideRunnable, 3000L)
                    }
                    AgentState.ERROR -> {
                        handler.removeCallbacks(hideRunnable)
                        handler.postDelayed(hideRunnable, 4000L)
                    }
                }
            }
        }
    }

    fun showFace() {
        handler.removeCallbacks(hideRunnable)
        showFaceWithAnimation()
    }

    private fun showFaceWithAnimation() {
        composeView?.let { view ->
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(250)
                    .setListener(null)
                    .start()
            }
        }
    }

    private fun hideFaceWithAnimation() {
        composeView?.let { view ->
            if (view.visibility == View.VISIBLE) {
                view.animate()
                    .scaleX(0.0f)
                    .scaleY(0.0f)
                    .setDuration(250)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            view.visibility = View.GONE
                            RoboPalApplication.agentEngine.clearHistory()
                        }
                    })
                    .start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        handler.removeCallbacks(hideRunnable)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()

        composeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removiendo vista overlay: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
