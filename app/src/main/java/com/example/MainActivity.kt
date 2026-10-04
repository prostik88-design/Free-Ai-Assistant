package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.chat.ChatsListScreen
import com.example.ui.screens.mcp.McpScreen
import com.example.ui.screens.projects.ProjectsScreen
import com.example.ui.screens.skills.SkillsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.AppViewModel

enum class NavTab(val title: String) {
    CHATS("Chats"),
    PROJECTS("Projects"),
    SKILLS("Skills"),
    MCP("MCP"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val config = LocalConfiguration.current
                val isExpanded = config.screenWidthDp >= 600

                val settings by viewModel.userSettings.collectAsState()
                var hasEnteredApp by remember { mutableStateOf(false) }

                if (!hasEnteredApp && !settings.hasApiKey) {
                    AuthScreen(
                        viewModel = viewModel,
                        onContinue = { hasEnteredApp = true }
                    )
                } else {
                    RouterAgentApp(
                        viewModel = viewModel,
                        isExpandedScreen = isExpanded
                    )
                }
            }
        }
    }
}

@Composable
fun RouterAgentApp(
    viewModel: AppViewModel,
    isExpandedScreen: Boolean
) {
    var currentTab by remember { mutableStateOf(NavTab.CHATS) }
    var activeChatId by remember { mutableStateOf<String?>(null) }

    val currentChatId = activeChatId

    // Handle back button when in detail view
    BackHandler(enabled = currentChatId != null) {
        activeChatId = null
    }

    if (currentChatId != null) {
        ChatDetailScreen(
            chatId = currentChatId,
            viewModel = viewModel,
            onNavigateBack = { activeChatId = null }
        )
    } else {
        if (isExpandedScreen) {
            // Adaptive Tablet/Desktop layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    NavTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { NavTabIcon(tab) },
                            label = { Text(tab.title) },
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    TabContent(
                        tab = currentTab,
                        viewModel = viewModel,
                        onOpenChat = { activeChatId = it },
                        onNavigateToSettings = { currentTab = NavTab.SETTINGS }
                    )
                }
            }
        } else {
            // Standard Mobile Scaffold with Bottom NavigationBar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("main_navigation_bar")
                    ) {
                        NavTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { NavTabIcon(tab) },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    TabContent(
                        tab = currentTab,
                        viewModel = viewModel,
                        onOpenChat = { activeChatId = it },
                        onNavigateToSettings = { currentTab = NavTab.SETTINGS }
                    )
                }
            }
        }
    }
}

@Composable
fun NavTabIcon(tab: NavTab) {
    when (tab) {
        NavTab.CHATS -> Icon(Icons.Default.ChatBubbleOutline, contentDescription = tab.title)
        NavTab.PROJECTS -> Icon(Icons.Default.Folder, contentDescription = tab.title)
        NavTab.SKILLS -> Icon(Icons.Default.Extension, contentDescription = tab.title)
        NavTab.MCP -> Icon(Icons.Default.Dns, contentDescription = tab.title)
        NavTab.SETTINGS -> Icon(Icons.Default.Settings, contentDescription = tab.title)
    }
}

@Composable
fun TabContent(
    tab: NavTab,
    viewModel: AppViewModel,
    onOpenChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    when (tab) {
        NavTab.CHATS -> ChatsListScreen(
            viewModel = viewModel,
            onOpenChat = onOpenChat,
            onNavigateToSettings = onNavigateToSettings
        )
        NavTab.PROJECTS -> ProjectsScreen(
            viewModel = viewModel,
            onStartProjectChat = onOpenChat
        )
        NavTab.SKILLS -> SkillsScreen(
            viewModel = viewModel,
            onBindSkillToChat = onOpenChat
        )
        NavTab.MCP -> McpScreen(viewModel = viewModel)
        NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
    }
}
