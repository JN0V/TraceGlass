package io.github.jn0v.traceglass.feature.tracing.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit,
    onReopenOnboarding: () -> Unit = {},
    onSetupGuide: () -> Unit = {},
    onAbout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        SettingsContent(
            uiState = uiState,
            onAudioFeedbackToggled = viewModel::onAudioFeedbackToggled,
            onBreakReminderToggled = viewModel::onBreakReminderToggled,
            onBreakIntervalChanged = viewModel::onBreakIntervalChanged,
            onReopenOnboarding = onReopenOnboarding,
            onSetupGuide = onSetupGuide,
            onAbout = onAbout,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onAudioFeedbackToggled: (Boolean) -> Unit,
    onBreakReminderToggled: (Boolean) -> Unit,
    onBreakIntervalChanged: (Int) -> Unit,
    onReopenOnboarding: () -> Unit,
    onSetupGuide: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Audio feedback") },
            supportingContent = { Text("Play sound on tracking state changes") },
            trailingContent = {
                Switch(
                    checked = uiState.audioFeedbackEnabled,
                    onCheckedChange = onAudioFeedbackToggled,
                    modifier = Modifier.semantics {
                        contentDescription = "Audio feedback toggle"
                    }
                )
            }
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Break reminders") },
            supportingContent = { Text("Remind to take breaks during tracing") },
            trailingContent = {
                Switch(
                    checked = uiState.breakReminderEnabled,
                    onCheckedChange = onBreakReminderToggled,
                    modifier = Modifier.semantics {
                        contentDescription = "Break reminder toggle"
                    }
                )
            }
        )

        AnimatedVisibility(visible = uiState.breakReminderEnabled) {
            ListItem(
                headlineContent = {
                    Text("Reminder interval: ${uiState.breakReminderIntervalMinutes} min")
                },
                supportingContent = {
                    Slider(
                        value = uiState.breakReminderIntervalMinutes.toFloat(),
                        onValueChange = { onBreakIntervalChanged(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10,
                        modifier = Modifier.semantics {
                            contentDescription = "Break reminder interval slider"
                        }
                    )
                }
            )
        }

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Setup guide") },
            supportingContent = { Text("Marker placement and phone stand tips") },
            modifier = Modifier.clickable(onClick = onSetupGuide)
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Re-open onboarding") },
            supportingContent = { Text("Review the full onboarding walkthrough") },
            modifier = Modifier.clickable(onClick = onReopenOnboarding)
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("About") },
            supportingContent = { Text("App info and licenses") },
            modifier = Modifier.clickable(onClick = onAbout)
        )
    }
}
