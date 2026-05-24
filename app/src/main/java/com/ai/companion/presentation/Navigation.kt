package com.ai.companion.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ai.companion.presentation.base.BackgroundWhite
import com.ai.companion.presentation.base.PrimaryGreen
import com.ai.companion.presentation.base.TextSecondary
import com.ai.companion.presentation.chat.ChatScreen
import com.ai.companion.presentation.chat.ChatViewModel
import com.ai.companion.presentation.memory.MemoryScreen
import com.ai.companion.presentation.settings.SettingsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Chat : Screen("chat", "聊天", Icons.Default.Chat)
    object Memory : Screen("memory", "记忆", Icons.Default.History)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@Composable
fun AICompanionApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigation(navController = navController) }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier
                .background(BackgroundWhite)
                .padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigation(navController: NavHostController) {
    val screens = listOf(
        Screen.Chat,
        Screen.Memory,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = BackgroundWhite
    ) {
        screens.forEach { screen ->
            AddItem(
                screen = screen,
                currentDestination = currentDestination,
                navController = navController
            )
        }
    }
}

@Composable
fun RowScope.AddItem(
    screen: Screen,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

    NavigationBarItem(
        label = {
            Text(
                text = screen.title,
                color = if (selected) PrimaryGreen else TextSecondary
            )
        },
        icon = {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                tint = if (selected) PrimaryGreen else TextSecondary
            )
        },
        selected = selected,
        onClick = {
            navController.navigate(screen.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = PrimaryGreen.copy(alpha = 0.1f)
        )
    )
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        modifier = modifier
    ) {
        composable(Screen.Chat.route) {
            ChatScreen()
        }

        composable(Screen.Memory.route) {
            MemoryScreen(
                onSessionClick = { sessionId ->
                    navController.navigate("${Screen.Chat.route}/$sessionId")
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // 带sessionId的聊天页面
        composable("${Screen.Chat.route}/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val viewModel: ChatViewModel = hiltViewModel(
                creator = { viewModelStore ->
                    val savedStateHandle = androidx.lifecycle.SavedStateHandle(
                        mapOf("sessionId" to sessionId)
                    )
                    ChatViewModel(
                        chatUseCases = hiltViewModel<ChatViewModel>().chatUseCases,
                        savedStateHandle = savedStateHandle
                    )
                }
            )
            ChatScreen(viewModel = viewModel)
        }
    }
}
