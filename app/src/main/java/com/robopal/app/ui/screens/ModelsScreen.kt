package com.robopal.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.robopal.app.managers.DownloadStatus
import com.robopal.app.managers.HuggingFaceClient
import com.robopal.app.managers.HuggingFaceModel
import com.robopal.app.managers.ModelDownloadState
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

    val downloadStates by downloadManager.downloadStates.collectAsState()

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
                val dlState = downloadStates[model.taskFileName] ?: downloadManager.getDownloadState(model.taskFileName)

                ModelItemCard(
                    model = model,
                    downloadState = dlState,
                    installedFiles = installedFiles,
                    onDownloadClick = {
                        if (model.requiresLicense && !model.licenseUrl.isNullOrBlank()) {
                            Toast.makeText(context, "Acepta la licencia en Hugging Face y vuelve a intentar", Toast.LENGTH_LONG).show()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(model.licenseUrl))
                            context.startActivity(intent)
                        } else {
                            val targetFileName = model.taskFileName
                            CoroutineScope(Dispatchers.IO).launch {
                                val res = downloadManager.downloadFile(model.downloadUrl, targetFileName)
                                if (!res.startsWith("Error")) {
                                    val targetFile = File(modelDir, targetFileName)
                                    if (targetFile.exists()) {
                                        llmManager.loadModel(targetFile)
                                    }
                                }
                            }
                        }
                    },
                    onCancelClick = {
                        downloadManager.cancelDownload(model.taskFileName)
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
    downloadState: ModelDownloadState,
    installedFiles: List<File>,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSelectClick: (File) -> Unit
) {
    val installedFile = installedFiles.firstOrNull { it.name.equals(model.taskFileName, ignoreCase = true) }
    val isInstalled = installedFile != null || downloadState.status == DownloadStatus.COMPLETED
    val isActive = RoboPalApplication.llmManager.activeModelFile?.name.equals(model.taskFileName, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = model.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        text = if (isInstalled) "Instalado (${(installedFile?.length() ?: downloadState.bytesDownloaded) / (1024 * 1024)} MB)"
                               else "Tamaño aprox: ${model.sizeBytes / (1024 * 1024)} MB",
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
                            onClick = {
                                val fileToLoad = installedFile ?: File(RoboPalApplication.llmManager.modelDirectory, model.taskFileName)
                                onSelectClick(fileToLoad)
                            }
                        ) {
                            Text("Usar", color = Color.White)
                        }
                    }
                } else {
                    when (downloadState.status) {
                        DownloadStatus.DOWNLOADING -> {
                            OutlinedButton(onClick = onCancelClick) {
                                Text("Cancelar", color = Color(0xFFFF5252))
                            }
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED, DownloadStatus.CORRUPTED -> {
                            Button(
                                onClick = onDownloadClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
                            ) {
                                Text("Reintentar")
                            }
                        }
                        else -> {
                            Button(
                                onClick = onDownloadClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
                            ) {
                                Text(if (model.requiresLicense) "Licencia / Descargar" else "Descargar")
                            }
                        }
                    }
                }
            }

            if (!isInstalled && downloadState.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (downloadState.totalBytes > 0) downloadState.bytesDownloaded.toFloat() / downloadState.totalBytes else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF7B61FF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${downloadState.bytesDownloaded / (1024 * 1024)} MB / ${downloadState.totalBytes / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = "${downloadState.percentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7B61FF)
                    )
                }
            }

            if (downloadState.errorMessage != null && downloadState.status != DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = downloadState.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF5252)
                )
            }
        }
    }
}
