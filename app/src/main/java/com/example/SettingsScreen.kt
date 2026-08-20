package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager, viewModel: ChatViewModel) {
    val theme by settingsManager.themeState.collectAsStateWithLifecycle()
    val voice by settingsManager.voiceResponse.collectAsStateWithLifecycle()
    val history by settingsManager.saveHistory.collectAsStateWithLifecycle()
    
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Settings",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Appearance
            Text("APPEARANCE", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column {
                    SettingsRow(
                        icon = { Icon(Icons.Outlined.Lightbulb, null, tint = TextSecondary) },
                        title = "Theme",
                        subtitle = "$theme >"
                    ) {
                        // In a real app, open a dialog to select theme.
                        // For simplicity here, cycle through.
                        val next = when (theme) {
                            "Dark" -> "Light"
                            "Light" -> "System"
                            else -> "Dark"
                        }
                        settingsManager.setTheme(next)
                    }
                    HorizontalDivider(color = SurfaceSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        icon = { Icon(Icons.Outlined.DarkMode, null, tint = TextSecondary) },
                        title = "Dark Mode",
                        checked = theme == "Dark" || theme == "System"
                    ) {
                        settingsManager.setTheme(if (it) "Dark" else "Light")
                    }
                    HorizontalDivider(color = SurfaceSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        icon = { Icon(Icons.Outlined.LightMode, null, tint = TextSecondary) },
                        title = "Light Mode",
                        checked = theme == "Light"
                    ) {
                        settingsManager.setTheme(if (it) "Light" else "Dark")
                    }
                    HorizontalDivider(color = SurfaceSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        icon = { Icon(Icons.Outlined.SettingsSystemDaydream, null, tint = TextSecondary) },
                        title = "System Default",
                        checked = theme == "System"
                    ) {
                        if (it) settingsManager.setTheme("System") else settingsManager.setTheme("Dark")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Chat
            Text("CHAT", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column {
                    SettingsSwitchRow(
                        icon = { Icon(Icons.Outlined.History, null, tint = TextSecondary) },
                        title = "Chat History",
                        checked = history
                    ) {
                        settingsManager.setSaveHistory(it)
                    }
                    HorizontalDivider(color = SurfaceSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        icon = { Icon(Icons.Outlined.VolumeUp, null, tint = TextSecondary) },
                        title = "Voice Responses",
                        checked = voice
                    ) {
                        settingsManager.setVoiceResponse(it)
                    }
                    HorizontalDivider(color = SurfaceSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = { Icon(Icons.Outlined.Delete, null, tint = TextSecondary) },
                        title = "Clear Conversations",
                        titleColor = TextPrimary
                    ) {
                        showClearDialog = true
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App
            Text("APP", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column {
                    SettingsRow(
                        icon = { Icon(Icons.Outlined.Info, null, tint = TextSecondary) },
                        title = "About USMAN AI",
                        subtitle = ">"
                    ) {}
                    HorizontalDivider(color = SurfaceSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = { Icon(Icons.Outlined.VerifiedUser, null, tint = TextSecondary) },
                        title = "Version",
                        subtitle = "1.0.0"
                    ) {}
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to delete all chat history? This cannot be undone.") },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) {
                    Text("Clear", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    titleColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = titleColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (subtitle != null) {
            Text(subtitle, color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: @Composable () -> Unit,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceSecondary,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
