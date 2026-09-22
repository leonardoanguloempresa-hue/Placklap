package com.robopal.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.robopal.app.agent.AgentEngine
import com.robopal.app.agent.AgentState
import com.robopal.app.agent.Message
import com.robopal.app.ui.screens.ChatScreen
import com.robopal.app.ui.screens.HomeScreen
import com.robopal.app.ui.screens.LogsScreen
import com.robopal.app.ui.screens.SettingsScreen
import com.robopal.app.ui.screens.ToolLogItem
import com.robopal.app.ui.theme.DarkBackground
import com.robopal.app.ui.theme.RobotPrimary
import com.robopal.app.ui.theme.SurfaceDark
import com.robopal.app.ui.theme.TextPrimary
import com.robopal.app.ui.theme.TextSecondary

sealed class NavRoute(val route: String, val title: String, val icon: ImageVector) {
    object Home : NavRoute("home", "Robot", Icons.Default.Home)
    object Chat : NavRoute("chat", "Chat", Icons.Default.QuestionAnswer)
    object Settings : NavRoute("settings", "Ajustes", Icons.Default.Settings)
    object Logs : NavRoute("logs", "Logs", Icons.Default.List)
}

@Composable
fun NavGraph(
    agentEngine: AgentEngine,
    agentState: AgentState,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val items = listOf(
        NavRoute.Home,
        NavRoute.Chat,
        NavRoute.Settings,
        NavRoute.Logs
    )

    // Historial y logs para la UI
    val chatMessages = remember { mutableStateListOf<Message>() }
    val toolLogs = remember { mutableStateListOf<ToolLogItem>() }
    val lastMessage = chatMessages.lastOrNull { it.role == "assistant" }?.content

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RobotPrimary,
                            selectedTextColor = RobotPrimary,
                            indicatorColor = DarkBackground,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Home.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(NavRoute.Home.route) {
                HomeScreen(
                    agentState = agentState,
                    lastMessage = lastMessage
                )
            }
            composable(NavRoute.Chat.route) {
                ChatScreen(
                    messages = chatMessages,
                    onSendMessage = { userGoal ->
                        chatMessages.add(Message(role = "user", content = userGoal))
                        // El agente procesa la meta del usuario
                    }
                )
            }
            composable(NavRoute.Settings.route) {
                SettingsScreen()
            }
            composable(NavRoute.Logs.route) {
                LogsScreen(logs = toolLogs)
            }
        }
    }
}
