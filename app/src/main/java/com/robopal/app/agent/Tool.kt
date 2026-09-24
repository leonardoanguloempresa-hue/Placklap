package com.robopal.app.agent

import com.robopal.app.safety.RiskLevel

interface Tool {
    val name: String
    val description: String
    val parameterSchema: Map<String, Any>
    val riskLevel: RiskLevel
        get() = RiskLevel.LOW
    val requiredPermissions: List<String>
        get() = emptyList()

    suspend fun validate(args: Map<String, Any>): String? = null
    suspend fun execute(args: Map<String, Any>): String
}
