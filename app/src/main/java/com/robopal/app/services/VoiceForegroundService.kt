package com.robopal.app.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.robopal.app.RoboPalApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class VoiceForegroundService : Service() {

    companion object {
        private const val TAG = "VoiceForegroundService"
        private const val CHANNEL_ID = "voice_foreground_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startHotwordListening()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        RoboPalApplication.voskManager.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun startHotwordListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ RECORD_AUDIO no concedido")
            return
        }

        Log.i("Vosk", "🎤 Hotword iniciado")
        serviceScope.launch(Dispatchers.IO) {
            try {
                val modelDir = RoboPalApplication.voskManager.ensureHotwordModel()
                Log.i("Vosk", "📦 Modelo: ${modelDir.absolutePath}")

                RoboPalApplication.voskManager.initialize(modelDir)
                Log.i("Vosk", "✅ Reconocedor activo")

                val grammarList = listOf("silf", "sirf", "sulf", "sil", "solf", "oye silf", "hey silf", "ok silf", "[unk]")
                RoboPalApplication.voskManager.startListening(grammar = grammarList)

                val triggers = listOf("silf", "sirf", "sulf", "oye silf", "hey silf", "ok silf")

                RoboPalApplication.voskManager.finalText.collect { transcribedText ->
                    Log.i("Vosk", "📝 Detectado: '$transcribedText'")
                    val lower = transcribedText.lowercase().trim()
                    val matchedTrigger = triggers.firstOrNull { lower.contains(it) } ?: return@collect

                    val idx = lower.indexOf(matchedTrigger)
                    val comando = if (idx >= 0) {
                        transcribedText.substring(idx + matchedTrigger.length).trim()
                    } else ""

                    Log.i("Vosk", "🎯 Comando: '$comando'")

                    if (comando.length < 3) {
                        Log.i(TAG, "Solo hotword. Esperando comando 5s...")
                        val siguiente = withTimeoutOrNull(5000L) {
                            try {
                                RoboPalApplication.voskManager.finalText.first { texto ->
                                    val t = texto.lowercase().trim()
                                    t.length > 3 && triggers.none { t.contains(it) }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (!siguiente.isNullOrBlank()) {
                            onSilfCommandDetected(siguiente)
                        } else {
                            Log.i(TAG, "Sin comando. Volviendo a pasivo.")
                            startHotwordListening()
                        }
                    } else {
                        onSilfCommandDetected(comando)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error escuchando en VoiceForegroundService: ${e.message}", e)
            }
        }
    }

    private fun onSilfCommandDetected(prompt: String) {
        Log.i(TAG, "Comando 'Silf' detectado con prompt: $prompt")
        serviceScope.launch(Dispatchers.Main) {
            OverlayService.instance?.showFace()
            RoboPalApplication.agentEngine.agentLoop(prompt)
        }
        serviceScope.launch(Dispatchers.IO) {
            delay(2000)
            startHotwordListening()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Voz Persistente RoboPal",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la escucha activa continua del comando 'Silf' en segundo plano."
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
            .setContentText("Micrófono activo en segundo plano...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
