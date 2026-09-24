package com.robopal.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.robopal.app.agent.AgentState
import com.robopal.app.agent.Message
import com.robopal.app.services.OverlayService
import com.robopal.app.ui.robot.RobotFace
import com.robopal.app.ui.theme.DarkCard
import com.robopal.app.ui.theme.PureBlack
import com.robopal.app.ui.theme.RobotError
import com.robopal.app.ui.theme.RobotPrimary
import com.robopal.app.ui.theme.SubtleBorder
import com.robopal.app.ui.theme.TextPrimary
import com.robopal.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    agentState: AgentState,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val isModelReady by RoboPalApplication.llmManager.isReady.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        // Título minimalista
        Text(
            text = "Silf",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // FIX 5: Banner de advertencia si el modelo MediaPipe .task no está cargado
        if (!isModelReady) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RobotError.copy(alpha = 0.15f))
                    .border(1.dp, RobotError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Advertencia Modelo",
                    tint = RobotError,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚠️ Modelo no cargado — Ve a Modelos y descarga Qwen2.5-1.5B-Instruct (.task)",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Conversación con flujo de texto limpio
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                CleanMessageRow(message = message)
            }

            // Indicador de procesamiento con bola flotante miniatura
            if (agentState == AgentState.THINKING || agentState == AgentState.WORKING || agentState == AgentState.LISTENING) {
                item {
                    MiniRobotProcessingRow(agentState = agentState)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barra de entrada flotante en forma de píldora
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
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Escribe o di 'Silf'...", color = TextSecondary, fontSize = 14.sp) },
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

            // Botón de micrófono manual
            IconButton(
                onClick = {
                    OverlayService.instance?.showFace()
                    scope.launch {
                        RoboPalApplication.agentEngine.agentLoop("Escribe un mensaje en pantalla o saluda.")
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Micrófono",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(RobotPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CleanMessageRow(message: Message) {
    val senderName = when (message.role) {
        "user" -> "Tú"
        "assistant" -> "Silf"
        "tool" -> "Acción Herramienta"
        else -> message.role
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = senderName,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (message.role == "assistant") RobotPrimary else TextSecondary,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = message.content,
            fontSize = 15.sp,
            color = TextPrimary,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun MiniRobotProcessingRow(agentState: AgentState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
        ) {
            RobotFace(agentState = agentState)
        }

        Spacer(modifier = Modifier.width(10.dp))

        val labelText = when (agentState) {
            AgentState.LISTENING -> "Silf está escuchando..."
            AgentState.THINKING -> "Silf está pensando..."
            else -> "Silf está ejecutando..."
        }

        Text(
            text = labelText,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}
