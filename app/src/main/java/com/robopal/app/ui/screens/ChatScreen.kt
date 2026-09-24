package com.robopal.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.AgentState
import com.robopal.app.agent.Message
import com.robopal.app.ui.robot.RobotFace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val agentEngine = RoboPalApplication.agentEngine
    val voskManager = RoboPalApplication.voskManager

    val messages by agentEngine.agentMessages.collectAsState()
    val agentState by agentEngine.state.collectAsState()
    val partialText by voskManager.partialText.collectAsState()
    val isListening by voskManager.isListening.collectAsState()

    val bloqueado = agentState != AgentState.IDLE

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val modelDir = voskManager.ensureHotwordModel()
                    voskManager.initialize(modelDir)
                    voskManager.startListening(grammar = null)
                } catch (e: Exception) {
                    coroutineScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error cargando modelo de voz: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Permiso de micrófono denegado", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        voskManager.finalText.collect { transcribedText ->
            if (transcribedText.isNotBlank()) {
                agentEngine.agentLoop(transcribedText)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF121212)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RobotFace(
                    agentState = if (isListening) AgentState.LISTENING else agentState,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "RoboPal Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (isListening) "LISTENING: $partialText" else "Estado: ${agentState.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isListening -> Color(0xFFFFEB3B)
                            agentState == AgentState.THINKING || agentState == AgentState.WORKING -> Color(0xFFFF9800)
                            agentState == AgentState.ERROR -> Color(0xFFFF1744)
                            else -> Color(0xFFA0A0A0)
                        }
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(messages) { _, msg ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    ChatMessageBubble(message = msg)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!bloqueado) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            if (isListening) {
                                voskManager.stop()
                            } else {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val modelDir = voskManager.ensureHotwordModel()
                                        voskManager.initialize(modelDir)
                                        voskManager.startListening(grammar = null)
                                    } catch (e: Exception) {
                                        coroutineScope.launch(Dispatchers.Main) {
                                            Toast.makeText(context, "Error iniciando voz: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                enabled = !bloqueado,
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isListening) Color(0xFFFFEB3B) else Color(0xFF1E1E24), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Micrófono",
                    tint = if (isListening) Color.Black else Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                enabled = !bloqueado,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe una instrucción a RoboPal...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF121212),
                    disabledContainerColor = Color(0xFF0A0A0A),
                    focusedBorderColor = Color(0xFFE0E0E0),
                    unfocusedBorderColor = Color(0xFF333333),
                    disabledBorderColor = Color(0xFF222222),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.Gray
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !bloqueado) {
                        val prompt = inputText
                        inputText = ""
                        coroutineScope.launch {
                            agentEngine.agentLoop(prompt)
                        }
                    }
                },
                enabled = !bloqueado,
                modifier = Modifier
                    .size(48.dp)
                    .background(if (bloqueado) Color(0xFF1A1A22) else Color(0xFF2A2A35), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (bloqueado) Color.DarkGray else Color.White
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: Message) {
    val isUser = message.role.equals("user", ignoreCase = true)
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) Color(0xFF2A2A35) else Color(0xFF121212)
    val textColor = Color.White

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "Tú" else "RoboPal",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}
