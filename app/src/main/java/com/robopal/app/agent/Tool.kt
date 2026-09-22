package com.robopal.app.agent

interface Tool {
    val name: String
    val description: String
    val parameterSchema: Map<String, Any>

    suspend fun execute(args: Map<String, Any>): String
}
