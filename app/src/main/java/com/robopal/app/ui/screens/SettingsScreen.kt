package com.robopal.app.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robopal.app.ui.theme.DarkBackground
import com.robopal.app.ui.theme.RobotPrimary
import com.robopal.app.ui.theme.SurfaceDark
import com.robopal.app.ui.theme.TextPrimary
import com.robopal.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var confirmDangerousActions by remember { mutableStateOf(true) }

    val models = listOf(
        "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        "llama-3.2-1b-instruct-q4_k_m.gguf",
        "phi-3.5-mini-instruct-q4_k_m.gguf"
    )
    var selectedModel by remember { mutableStateOf(models[0]) }
    var expandedModelDropdown by remember { mutableStateOf(false) }

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

        // Permisos y Servicios
        Text(
            text = "Permisos y Servicios del Sistema",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = RobotPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SettingToggleItem(
            title = "Servicio de Accesibilidad",
            subtitle = "Permite a RoboPal interactuar con la pantalla (tocar, deslizar, leer nodos)",
            checked = accessibilityEnabled,
            onCheckedChange = { accessibilityEnabled = it }
        )

        SettingToggleItem(
            title = "Mostrar sobre otras aplicaciones (Overlay)",
            subtitle = "Muestra la cara del robot flotante sobre cualquier app",
            checked = overlayEnabled,
            onCheckedChange = { overlayEnabled = it }
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
                text = "Selecciona el archivo .gguf para inferencia local en dispositivo",
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
                        text = selectedModel,
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
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, color = TextPrimary) },
                            onClick = {
                                selectedModel = model
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
            .padding(12.dp),
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
