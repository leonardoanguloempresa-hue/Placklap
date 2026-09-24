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
        startListening()
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

    fun startListening() {
        Log.i(TAG, "VoiceForegroundService: 1. Iniciando servicio de escucha continua...")
        serviceScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Vosk: 1. ensureHotwordModel…")
                val modelDir = RoboPalApplication.voskManager.ensureHotwordModel()
                Log.i(TAG, "Vosk: 2. modelo en ${modelDir.absolutePath} — ${modelDir.listFiles()?.size ?: 0} archivos")

                RoboPalApplication.voskManager.initialize(modelDir)
                Log.i(TAG, "Vosk: 3. inicializado")

                val grammarList = listOf("silf", "sirf", "sulf", "sil", "solf", "oye silf", "hey silf", "ok silf", "[unk]")
                RoboPalApplication.voskManager.startListening(grammar = grammarList)
                Log.i(TAG, "Vosk: 4. escuchando con gramática: $grammarList")

                RoboPalApplication.voskManager.finalText.collect { transcribedText ->
                    val lower = transcribedText.lowercase().trim()
                    val triggers = listOf("silf", "sirf", "sulf", "oye silf", "hey silf", "ok silf")

                    val matchedTrigger = triggers.firstOrNull { lower.contains(it) } ?: return@collect

                    val comando = transcribedText.substringAfter(matchedTrigger, "").trim()

                    if (comando.length < 3) {
                        Log.i(TAG, "Solo hotword. Esperando comando por 5s...")
                        val siguienteComando = withTimeoutOrNull(5000L) {
                            var capturado: String? = null
                            RoboPalApplication.voskManager.finalText.collect { texto ->
                                val t = texto.lowercase().trim()
                                if (t.length > 3 && triggers.none { t.contains(it) }) {
                                    capturado = texto
                                }
                            }
                            capturado
                        }
                        if (!siguienteComando.isNullOrBlank()) {
                            onSilfCommandDetected(siguienteComando)
                        } else {
                            Log.i(TAG, "Sin comando tras Silf. Volviendo a modo pasivo.")
                        }
                    } else {
                        onSilfCommandDetected(comando)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando o escuchando en VoiceForegroundService: ${e.message}", e)
            }
        }
    }

    private fun onSilfCommandDetected(prompt: String) {
        Log.i(TAG, "Comando 'Silf' detectado con prompt: $prompt")
        serviceScope.launch(Dispatchers.Main) {
            OverlayService.instance?.showFace()
            RoboPalApplication.agentEngine.agentLoop(prompt)
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
