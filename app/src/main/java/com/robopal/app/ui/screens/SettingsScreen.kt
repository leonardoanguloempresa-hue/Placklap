package com.robopal.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robopal.app.RoboPalApplication
import com.robopal.app.services.AgentAccessibilityService
import com.robopal.app.ui.theme.DarkBackground
import com.robopal.app.ui.theme.RobotPrimary
import com.robopal.app.ui.theme.SurfaceDark
import com.robopal.app.ui.theme.TextPrimary
import com.robopal.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class GgufModelInfo(
    val name: String,
    val url: String,
    val fileName: String
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var accessibilityEnabled by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var confirmDangerousActions by remember { mutableStateOf(true) }

    val modelOptions = listOf(
        GgufModelInfo(
            name = "Qwen 1.5 1.8B Chat (Q4_K_M)",
            url = "https://huggingface.co/Qwen/Qwen1.5-1.8B-Chat-GGUF/resolve/main/qwen1_5-1_8b-chat-q4_k_m.gguf",
            fileName = "qwen1_5-1_8b-chat-q4_k_m.gguf"
        ),
        GgufModelInfo(
            name = "Llama 3.2 1B Instruct (Q4_K_M)",
            url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        ),
        GgufModelInfo(
            name = "Qwen 2.5 0.5B Instruct (Q4_K_M)",
            url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        )
    )

    var selectedModel by remember { mutableStateOf(modelOptions[0]) }
    var expandedModelDropdown by remember { mutableStateOf(false) }
    var downloadStatusText by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        accessibilityEnabled = AgentAccessibilityService.instance != null
        overlayEnabled = Settings.canDrawOverlays(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Ajustes y Configuración",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Permisos y Servicios del Sistema
        Text(
            text = "Permisos y Servicios del Sistema",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = RobotPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SettingToggleItem(
            title = "Servicio de Accesibilidad",
            subtitle = "Abre los ajustes para habilitar RoboPal (Tocar, deslizar, leer pantalla)",
            checked = accessibilityEnabled,
            onCheckedChange = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        SettingToggleItem(
            title = "Mostrar sobre otras aplicaciones (Overlay)",
            subtitle = "Abre los ajustes para autorizar la cara flotante sobre otras apps",
            checked = overlayEnabled,
            onCheckedChange = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.packageName)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Seguridad y Agente
        Text(
            text = "Seguridad y Agente",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = RobotPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SettingToggleItem(
            title = "Confirmar acciones peligrosas",
            subtitle = "Pide confirmación antes de instalar apps o realizar pagos",
            checked = confirmDangerousActions,
            onCheckedChange = { confirmDangerousActions = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Modelo LLM GGUF
        Text(
            text = "Modelo LLM Local (GGUF)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = RobotPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .padding(12.dp)
        ) {
            Text(
                text = "Modelo de IA seleccionado",
                fontSize = 15.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Descarga e instala el modelo .gguf para inferencia local offline",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedModelDropdown = true }
                ) {
                    Text(
                        text = selectedModel.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                DropdownMenu(
                    expanded = expandedModelDropdown,
                    onDismissRequest = { expandedModelDropdown = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    modelOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name, color = TextPrimary) },
                            onClick = {
                                selectedModel = option
                                expandedModelDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    isDownloading = true
                    downloadStatusText = "Iniciando descarga de ${selectedModel.fileName}..."
                    scope.launch {
                        val result = RoboPalApplication.downloadManager.downloadFile(
                            url = selectedModel.url,
                            destinationPath = "/sdcard/RoboPal/models/${selectedModel.fileName}"
                        )
                        downloadStatusText = result
                        isDownloading = false
                    }
                },
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = RobotPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isDownloading) "Descargando..." else "Descargar Modelo .gguf")
            }

            if (downloadStatusText.isNotBlank()) {
                Text(
                    text = downloadStatusText,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(12.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = RobotPrimary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkBackground
            )
        )
    }
}
