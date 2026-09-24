package com.robopal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robopal.app.RoboPalApplication
import com.robopal.app.managers.HuggingFaceClient
import com.robopal.app.managers.HuggingFaceModel
import com.robopal.app.ui.theme.DarkCard
import com.robopal.app.ui.theme.NeutralPrimary
import com.robopal.app.ui.theme.PureBlack
import com.robopal.app.ui.theme.SubtleBorder
import com.robopal.app.ui.theme.TextPrimary
import com.robopal.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier
) {
    val hfClient = remember { HuggingFaceClient() }
    val scope = rememberCoroutineScope()
    val downloadProgressState by RoboPalApplication.downloadManager.downloadProgress.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("MediaPipe (.task)", "Modelos Descargados")

    var searchQuery by remember { mutableStateOf("qwen") }
    var searchResults by remember { mutableStateOf<List<HuggingFaceModel>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var downloadingRepoId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    var localTaskFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var activeModelName by remember { mutableStateOf("") }

    fun refreshLocalModels() {
        val dir = RoboPalApplication.llmManager.modelDirectory
        if (dir.exists()) {
            val files = dir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }?.toList() ?: emptyList()
            localTaskFiles = files
            val active = RoboPalApplication.llmManager.activeModelFile
            if (active != null && files.contains(active)) {
                activeModelName = active.name
            } else if (files.isNotEmpty()) {
                activeModelName = files.first().name
                scope.launch {
                    try {
                        RoboPalApplication.llmManager.setActiveModelAndLoad(files.first())
                    } catch (e: Exception) {
                        statusMessage = "Error cargando modelo: ${e.message}"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshLocalModels()
        isSearching = true
        searchResults = hfClient.searchModels(searchQuery)
        isSearching = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "Modelos MediaPipe LLM (.task)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = PureBlack,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = NeutralPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        if (index == 1) refreshLocalModels()
                    },
                    text = { Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val progressInfo = downloadProgressState
        if (progressInfo != null && !progressInfo.isCompleted && progressInfo.error == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Descargando ${progressInfo.fileName}: ${progressInfo.percentage}%",
                    color = NeutralPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progressInfo.percentage / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = NeutralPrimary,
                    trackColor = PureBlack
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                color = NeutralPrimary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when (selectedTabIndex) {
            0 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(DarkCard)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar modelos .task...", color = TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                scope.launch {
                                    isSearching = true
                                    searchResults = hfClient.searchModels(searchQuery)
                                    isSearching = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(NeutralPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = PureBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSearching) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeutralPrimary)
                        Text("Buscando en HuggingFace...", color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                } else if (searchResults.isEmpty()) {
                    Text("No se encontraron modelos con '$searchQuery'.", color = TextSecondary)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { model ->
                            HuggingFaceModelCard(
                                model = model,
                                isDownloading = downloadingRepoId == model.id,
                                onDownload = {
                                    scope.launch {
                                        downloadingRepoId = model.id
                                        statusMessage = "Buscando archivo .task en ${model.id}..."
                                        val downloadUrl = hfClient.getTaskDownloadUrl(model.id)
                                        if (downloadUrl != null) {
                                            val fileName = downloadUrl.substringAfterLast("/")
                                            val targetFile = File(RoboPalApplication.llmManager.modelDirectory, fileName)
                                            statusMessage = "Iniciando descarga de $fileName..."
                                            val result = RoboPalApplication.downloadManager.downloadFile(downloadUrl, targetFile.absolutePath)
                                            statusMessage = result
                                            if (targetFile.exists()) {
                                                try {
                                                    RoboPalApplication.llmManager.setActiveModelAndLoad(targetFile)
                                                    statusMessage = "Modelo ${targetFile.name} cargado con éxito en MediaPipe."
                                                } catch (e: Exception) {
                                                    statusMessage = "Error cargando modelo: ${e.message}"
                                                }
                                            }
                                            refreshLocalModels()
                                        } else {
                                            statusMessage = "Error: No se encontró ningún archivo .task en el repositorio."
                                        }
                                        downloadingRepoId = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (localTaskFiles.isEmpty()) {
                    Text(
                        text = "Aún no hay modelos .task descargados en la carpeta de la app.",
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(localTaskFiles) { file ->
                            LocalModelCard(
                                file = file,
                                isActive = file.name == activeModelName,
                                onSelect = {
                                    scope.launch {
                                        try {
                                            RoboPalApplication.llmManager.setActiveModelAndLoad(file)
                                            activeModelName = file.name
                                            statusMessage = "Modelo cargado con éxito: ${file.name}"
                                        } catch (e: Exception) {
                                            statusMessage = "Error cargando modelo: ${e.message}"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HuggingFaceModelCard(
    model: HuggingFaceModel,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.id,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Descargas: ${model.downloads} | Me gusta: ${model.likes}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = NeutralPrimary, contentColor = PureBlack),
                shape = RoundedCornerShape(50)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Descargar", modifier = Modifier.size(16.dp), tint = PureBlack)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Obtener .task", fontSize = 12.sp, color = PureBlack)
                }
            }
        }
    }
}

@Composable
fun LocalModelCard(
    file: File,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isActive) NeutralPrimary else SubtleBorder, RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val sizeMb = file.length() / (1024 * 1024)
                Text(
                    text = "Tamaño: $sizeMb MB",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Modelo Activo",
                    tint = NeutralPrimary
                )
            } else {
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Activar", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}
