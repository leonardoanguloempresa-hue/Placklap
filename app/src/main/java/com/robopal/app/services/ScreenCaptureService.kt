package com.robopal.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder

class ScreenCaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 1002
        var instance: ScreenCaptureService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun captureBitmap(): Bitmap? {
        // Estructura preparada para la captura mediante ImageReader/MediaProjection
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Captura de Pantalla",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal activo para la captura de pantalla del agente RoboPal."
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
            .setContentTitle("RoboPal - Captura de Pantalla Activa")
            .setContentText("Procesando pantalla en segundo plano...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
