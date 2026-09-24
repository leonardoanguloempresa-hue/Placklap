package com.robopal.app.safety

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class ToolExecutionPolicy(
    val allowlistApps: Set<String> = emptySet(),
    val denylistApps: Set<String> = setOf("com.android.settings", "com.bank"),
    val maxConsecutiveActions: Int = 15,
    val timeoutMsPerTool: Long = 10_000L,
    val isDryRun: Boolean = false
)

interface RiskConfirmationHandler {
    suspend fun requestUserConfirmation(
        toolName: String,
        riskLevel: RiskLevel,
        arguments: Map<String, Any>,
        description: String
    ): Boolean
}

class AlwaysApproveConfirmationHandler : RiskConfirmationHandler {
    override suspend fun requestUserConfirmation(
        toolName: String,
        riskLevel: RiskLevel,
        arguments: Map<String, Any>,
        description: String
    ): Boolean = true
}
