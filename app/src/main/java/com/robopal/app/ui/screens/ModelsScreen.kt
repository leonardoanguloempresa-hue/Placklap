package com.robopal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.robopal.app.ui.theme.DarkBackground
import com.robopal.app.ui.theme.RobotPrimary
import com.robopal.app.ui.theme.SurfaceDark
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

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Explorador HuggingFace", "Modelos Descargados")

    // Estado del explorador
    var searchQuery by remember { mutableStateOf("qwen") }
    var searchResults by remember { mutableStateOf<List<HuggingFaceModel>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var downloadingRepoId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    // Estado de modelos locales
    var localGgufFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var activeModelName by remember { mutableStateOf("") }

    fun refreshLocalModels() {
        val dir = RoboPalApplication.llmManager.modelDirectory
        if (dir.exists()) {
            val files = dir.listFiles { _, name -> name.endsWith(".gguf", ignoreCase = true) }?.toList() ?: emptyList()
            localGgufFiles = files
            if (activeModelName.isBlank() && files.isNotEmpty()) {
                activeModelName = files.first().name
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
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Gestor de Modelos GGUF",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Pestañas
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = RobotPrimary
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
                    text = { Text(title, fontWeight = FontWeight.Medium) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                color = RobotPrimary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when (selectedTabIndex) {
            0 -> {
                // Explorador HuggingFace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar modelos GGUF...", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RobotPrimary,
                            unfocusedBorderColor = SurfaceDark,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

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
                            .clip(RoundedCornerShape(50))
                            .background(RobotPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSearching) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = RobotPrimary)
                        Text("Buscando en HuggingFace...", color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                } else if (searchResults.isEmpty()) {
                    Text("No se encontraron modelos con '$searchQuery'.", color = TextSecondary)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { model ->
                            HuggingFaceModelCard(
                                model = model,
                                isDownloading = downloadingRepoId == model.id,
                                onDownload = {
                                    scope.launch {
                                        downloadingRepoId = model.id
                                        statusMessage = "Buscando archivo .gguf en ${model.id}..."
                                        val downloadUrl = hfClient.getGgufDownloadUrl(model.id)
                                        if (downloadUrl != null) {
                                            val fileName = downloadUrl.substringAfterLast("/")
                                            statusMessage = "Descargando $fileName..."
                                            val destPath = File(RoboPalApplication.llmManager.modelDirectory, fileName).absolutePath
                                            val result = RoboPalApplication.downloadManager.downloadFile(downloadUrl, destPath)
                                            statusMessage = result
                                            refreshLocalModels()
                                        } else {
                                            statusMessage = "Error: No se encontró ningún archivo .gguf en el repositorio."
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
                // Lista de Modelos Descargados
                if (localGgufFiles.isEmpty()) {
                    Text(
                        text = "Aún no hay modelos .gguf descargados en el dispositivo.",
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(localGgufFiles) { file ->
                            LocalModelCard(
                                file = file,
                                isActive = file.name == activeModelName,
                                onSelect = {
                                    activeModelName = file.name
                                    statusMessage = "Modelo activo: ${file.name}"
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                colors = ButtonDefaults.buttonColors(containerColor = RobotPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Descargar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Descargar", fontSize = 12.sp)
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
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) RobotPrimary.copy(alpha = 0.25f) else SurfaceDark
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    tint = RobotPrimary
                )
            } else {
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Activar", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}
