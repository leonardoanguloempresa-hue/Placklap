package com.robopal.app.agent

import com.robopal.app.agent.tools.DownloadTool
import com.robopal.app.agent.tools.FindAndTapTool
import com.robopal.app.agent.tools.LongPressTool
import com.robopal.app.agent.tools.OpenAppTool
import com.robopal.app.agent.tools.OpenUrlTool
import com.robopal.app.agent.tools.PressBackTool
import com.robopal.app.agent.tools.PressHomeTool
import com.robopal.app.agent.tools.PressRecentTool
import com.robopal.app.agent.tools.ReadScreenOcrTool
import com.robopal.app.agent.tools.ReadScreenTool
import com.robopal.app.agent.tools.SwipeTool
import com.robopal.app.agent.tools.TakeScreenshotTool
import com.robopal.app.agent.tools.TapTool
import com.robopal.app.agent.tools.ToggleFlashlightTool
import com.robopal.app.agent.tools.TypeTextTool
import com.robopal.app.agent.tools.VideoAddSubtitlesTool
import com.robopal.app.agent.tools.VideoCutTool
import com.robopal.app.agent.tools.VideoMergeTool
import com.robopal.app.agent.tools.WaitTool
import com.robopal.app.managers.Logger

class ToolRegistry {

    private val tools: MutableMap<String, Tool> = mutableMapOf()

    init {
        registerTool(TapTool())
        registerTool(SwipeTool())
        registerTool(LongPressTool())
        registerTool(TypeTextTool())
        registerTool(PressBackTool())
        registerTool(PressHomeTool())
        registerTool(PressRecentTool())
        registerTool(ReadScreenTool())
        registerTool(ReadScreenOcrTool())
        registerTool(OpenAppTool())
        registerTool(FindAndTapTool())
        registerTool(WaitTool())
        registerTool(VideoCutTool())
        registerTool(VideoMergeTool())
        registerTool(VideoAddSubtitlesTool())
        registerTool(DownloadTool())
        registerTool(OpenUrlTool())
        registerTool(TakeScreenshotTool())
        registerTool(ToggleFlashlightTool())
    }

    fun registerTool(tool: Tool) {
        tools[tool.name] = tool
    }

    fun getTool(name: String): Tool? {
        return tools[name]
    }

    fun getAllTools(): List<Tool> {
        return tools.values.toList()
    }

    suspend fun executeTool(name: String, args: Map<String, Any>): String {
        val tool = getTool(name)
            ?: run {
                val errorMsg = "Error: Herramienta '$name' no encontrada en el catálogo."
                Logger.logToolExecution(name, args, errorMsg)
                return errorMsg
            }
        val result = try {
            tool.execute(args)
        } catch (e: Exception) {
            "Error al ejecutar la herramienta '$name': ${e.localizedMessage ?: e.message}"
        }
        Logger.logToolExecution(name, args, result)
        return result
    }
}
