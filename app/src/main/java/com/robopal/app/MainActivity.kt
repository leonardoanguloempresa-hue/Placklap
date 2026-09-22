package com.robopal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.robopal.app.agent.AgentEngine
import com.robopal.app.agent.AgentState
import com.robopal.app.ui.navigation.NavGraph
import com.robopal.app.ui.theme.RoboPalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RoboPalTheme {
                val agentEngine = remember { AgentEngine() }
                val agentState by agentEngine.state.collectAsState(initial = AgentState.IDLE)

                NavGraph(
                    agentEngine = agentEngine,
                    agentState = agentState
                )
            }
        }
    }
}
