package com.robopal.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.robopal.app.agent.AgentState
import com.robopal.app.services.OverlayService
import com.robopal.app.services.VoiceForegroundService
import com.robopal.app.ui.navigation.NavGraph
import com.robopal.app.ui.theme.RoboPalTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        startVoiceAndOverlayServices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestMicrophonePermission()
        startVoiceAndOverlayServices()

        setContent {
            RoboPalTheme {
                val agentEngine = remember { RoboPalApplication.agentEngine }
                val agentState by agentEngine.state.collectAsState(initial = AgentState.IDLE)

                NavGraph(
                    agentEngine = agentEngine,
                    agentState = agentState
                )
            }
        }
    }

    private fun checkAndRequestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceAndOverlayServices() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val voiceIntent = Intent(this, VoiceForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(voiceIntent)
            } else {
                startService(voiceIntent)
            }
        }

        if (Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(overlayIntent)
            } else {
                startService(overlayIntent)
            }
        }
    }
}
