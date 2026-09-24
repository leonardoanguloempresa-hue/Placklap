package com.robopal.app.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var accessibilityEnabled by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var confirmDangerousActions by remember { mutableStateOf(true) }

    var localTaskFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedModelName by remember { mutableStateOf("") }
    var expandedModelDropdown by remember { mutableStateOf(false) }

    fun isAccessibilityServiceEnabled(ctx: Context): Boolean {
        if (AgentAccessibilityService.instance != null) return true
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == ctx.packageName }
    }

    fun refreshState() {
        accessibilityEnabled = isAccessibilityServiceEnabled(context)
        overlayEnabled = Settings.canDrawOverlays(context)

        val dir = RoboPalApplication.llmManager.modelDirectory
        if (dir.exists()) {
            val files = dir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }?.toList() ?: emptyList()
            localTaskFiles = files
            val active = RoboPalApplication.llmManager.activeModelFile
            if (active != null && files.contains(active)) {
                selectedModelName = active.name
            } else if (files.isNotEmpty()) {
                selectedModelName = files.first().name
                scope.launch {
                    try {
                        RoboPalApplication.llmManager.setActiveModelAndLoad(files.first())
                    } catch (_: Exception) {}
                }
            } else {
                selectedModelName = "No hay modelos .task locales"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshState()
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

        ReactivePermissionToggle(
            title = "Servicio de Accesibilidad",
            subtitle = if (accessibilityEnabled) "Concedido: RoboPal tiene permiso de accesibilidad." else "Requerido: Abre los ajustes para habilitar RoboPal.",
            isGranted = accessibilityEnabled,
            onGrantClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        ReactivePermissionToggle(
            title = "Mostrar sobre otras aplicaciones (Overlay)",
            subtitle = if (overlayEnabled) "Concedido: Permiso de ventana flotante activo." else "Requerido: Abre los ajustes para autorizar la cara flotante.",
            isGranted = overlayEnabled,
            onGrantClick = {
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

        // Seguridad
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

        // Modelo Activo
        Text(
            text = "Modelo LLM Activo",
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
                text = "Seleccionar Modelo .task Cargado",
                fontSize = 15.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Define qué archivo físico usará el LlmManager para MediaPipe LLM",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = localTaskFiles.isNotEmpty()) { expandedModelDropdown = true }
                ) {
                    Text(
                        text = selectedModelName,
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
                    localTaskFiles.forEach { file ->
                        DropdownMenuItem(
                            text = { Text(file.name, color = TextPrimary) },
                            onClick = {
                                selectedModelName = file.name
                                scope.launch {
                                    try {
                                        RoboPalApplication.llmManager.setActiveModelAndLoad(file)
                                    } catch (_: Exception) {}
                                }
                                expandedModelDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReactivePermissionToggle(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(12.dp)
            .clickable(enabled = !isGranted) { onGrantClick() },
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
                color = if (isGranted) RobotPrimary else TextSecondary
            )
        }
        Switch(
            checked = isGranted,
            enabled = !isGranted,
            onCheckedChange = { if (!isGranted) onGrantClick() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = RobotPrimary,
                disabledCheckedThumbColor = TextPrimary,
                disabledCheckedTrackColor = RobotPrimary.copy(alpha = 0.6f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkBackground
            )
        )
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
