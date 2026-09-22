package com.robopal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robopal.app.ui.theme.DarkBackground
import com.robopal.app.ui.theme.RobotPrimary
import com.robopal.app.ui.theme.SurfaceDark
import com.robopal.app.ui.theme.TextPrimary
import com.robopal.app.ui.theme.TextSecondary

data class ToolLogItem(
    val timestamp: String,
    val toolName: String,
    val args: String,
    val result: String
)

@Composable
fun LogsScreen(
    logs: List<ToolLogItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Registro de Herramientas (Logs)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Aún no se ha ejecutado ninguna herramienta.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { log ->
                    LogCard(log = log)
                }
            }
        }
    }
}

@Composable
fun LogCard(log: ToolLogItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(12.dp)
    ) {
        Text(
            text = "[${log.timestamp}] ${log.toolName}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = RobotPrimary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Argumentos: ${log.args}",
            fontSize = 12.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "Resultado: ${log.result}",
            fontSize = 12.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
