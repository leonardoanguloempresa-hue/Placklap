package com.robopal.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.robopal.app.RoboPalApplication
import com.robopal.app.managers.HuggingFaceClient
import com.robopal.app.managers.HuggingFaceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadManager = RoboPalApplication.downloadManager
    val llmManager = RoboPalApplication.llmManager

    val downloadProgressState by downloadManager.downloadProgress.collectAsState()

    val modelDir = llmManager.modelDirectory
    val installedFiles = modelDir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }?.toList() ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Catálogo de Modelos .task",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = "Descarga modelos Instruct en formato MediaPipe .task para la inferencia en chip.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        downloadProgressState?.let { dp ->
            if (!dp.isCompleted && dp.error == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Descargando ${dp.fileName}: ${dp.percentage}%", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { dp.percentage / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF7B61FF)
                        )
                    }
                }
            }
        }

        Text(
            text = "Modelos Recomendados (Instruct)",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(HuggingFaceClient.CURATED_MODELS) { model ->
                ModelItemCard(
                    model = model,
                    installedFiles = installedFiles,
                    isDownloading = downloadProgressState != null && !(downloadProgressState?.isCompleted ?: true),
                    onDownloadClick = {
                        if (model.requiresLicense && !model.licenseUrl.isNullOrBlank()) {
                            Toast.makeText(context, "Acepta la licencia en Hugging Face y vuelve a intentar", Toast.LENGTH_LONG).show()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(model.licenseUrl))
                            context.startActivity(intent)
                        } else {
                            val targetFile = File(modelDir, model.taskFileName)
                            CoroutineScope(Dispatchers.IO).launch {
                                val res = downloadManager.downloadFile(model.downloadUrl, targetFile.absolutePath)
                                if (!res.startsWith("Error")) {
                                    llmManager.loadModel(targetFile)
                                }
                            }
                        }
                    },
                    onSelectClick = { file ->
                        CoroutineScope(Dispatchers.IO).launch {
                            llmManager.loadModel(file)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ModelItemCard(
    model: HuggingFaceModel,
    installedFiles: List<File>,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onSelectClick: (File) -> Unit
) {
    val installedFile = installedFiles.firstOrNull { it.name.equals(model.taskFileName, ignoreCase = true) }
    val isInstalled = installedFile != null
    val isActive = RoboPalApplication.llmManager.activeModelFile?.name.equals(model.taskFileName, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = model.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    text = if (isInstalled) "Instalado (${installedFile?.length()?.div(1024 * 1024)} MB)" else "Tamaño aprox: ${model.sizeBytes / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) Color(0xFF4CAF50) else Color.Gray
                )
                if (model.requiresLicense) {
                    Text(
                        text = "⚠️ Requiere aceptar licencia en HF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isInstalled) {
                if (isActive) {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF2A2A35))
                    ) {
                        Text("Activo", color = Color(0xFF4CAF50))
                    }
                } else {
                    OutlinedButton(
                        onClick = { installedFile?.let { onSelectClick(it) } }
                    ) {
                        Text("Usar", color = Color.White)
                    }
                }
            } else {
                Button(
                    onClick = onDownloadClick,
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
                ) {
                    Text(if (model.requiresLicense) "Licencia / Descargar" else "Descargar")
                }
            }
        }
    }
}
