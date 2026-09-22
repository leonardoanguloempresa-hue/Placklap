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
import com.robopal.app.managers.PorcupineManager
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
    private var porcupineManager: PorcupineManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        porcupineManager = PorcupineManager(this) {
            onHotwordDetected()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        porcupineManager?.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun startListening() {
        Log.d(TAG, "VoiceForegroundService: Escuchando activamente palabra clave 'Robot'.")
        porcupineManager?.start()
    }

    private fun onHotwordDetected() {
        Log.d(TAG, "Palabra clave 'Robot' detectada. Transcribiendo comando con Vosk...")
        serviceScope.launch {
            val audioBytes = ByteArray(0)
            val userText = RoboPalApplication.voskManager.transcribeAudio(audioBytes)

            if (userText.isNotBlank()) {
                Log.d(TAG, "Iniciando bucle de agente con texto: $userText")
                RoboPalApplication.agentEngine.agentLoop(userText)

                val summary = "Comando completado por RoboPal."
                RoboPalApplication.ttsManager.speak(summary)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Voz de RoboPal",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal persistente para el reconocimiento y servicio de voz del agente RoboPal."
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
            .setContentTitle("RoboPal - Agente Activo")
            .setContentText("Escuchando palabra clave 'Robot'...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
