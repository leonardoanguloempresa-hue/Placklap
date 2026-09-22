package com.robopal.app.managers

import com.robopal.app.ui.screens.ToolLogItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val _logs = MutableStateFlow<List<ToolLogItem>>(emptyList())
    val logs: StateFlow<List<ToolLogItem>> = _logs.asStateFlow()

    fun logToolExecution(toolName: String, args: Map<String, Any>, result: String) {
        val timestamp = dateFormat.format(Date())
        val item = ToolLogItem(
            timestamp = timestamp,
            toolName = toolName,
            args = args.toString(),
            result = result
        )
        _logs.value = listOf(item) + _logs.value
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
