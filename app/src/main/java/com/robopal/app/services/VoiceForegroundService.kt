package com.robopal.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.robopal.app.RoboPalApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VoiceForegroundService : Service() {

    companion object {
        private const val TAG = "VoiceForegroundService"
        private const val CHANNEL_ID = "voice_foreground_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        RoboPalApplication.voskManager.stopContinuousListening()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun startListening() {
        Log.d(TAG, "VoiceForegroundService: Escuchando continuamente por el comando 'Silf'.")
        RoboPalApplication.voskManager.startContinuousListening { prompt ->
            onSilfCommandDetected(prompt)
        }
    }

    private fun onSilfCommandDetected(prompt: String) {
        Log.d(TAG, "Comando 'Silf' detectado con prompt: $prompt")
        serviceScope.launch {
            if (!RoboPalApplication.llmManager.isModelAvailable()) {
                val warning = "Por favor selecciona y descarga un modelo GGUF antes de dar comandos."
                RoboPalApplication.ttsManager.speak(warning)
                return@launch
            }

            RoboPalApplication.agentEngine.agentLoop(prompt)
            val summary = "Comando procesado por RoboPal."
            RoboPalApplication.ttsManager.speak(summary)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Voz de RoboPal",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal persistente para el reconocimiento de comando 'Silf' de RoboPal."
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
            .setContentTitle("RoboPal - Escuchando 'Silf'")
            .setContentText("Di 'Silf' seguido de tu comando...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
