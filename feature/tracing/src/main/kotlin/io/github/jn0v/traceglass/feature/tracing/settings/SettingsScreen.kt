package io.github.jn0v.traceglass.feature.tracing.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jn0v.traceglass.feature.tracing.R
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigation_back)
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
internal fun SettingsContent(
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
            headlineContent = { Text(stringResource(R.string.settings_audio_feedback)) },
            supportingContent = { Text(stringResource(R.string.settings_audio_feedback_desc)) },
            trailingContent = {
                Switch(
                    checked = uiState.audioFeedbackEnabled,
                    onCheckedChange = null
                )
            },
            modifier = Modifier.toggleable(
                value = uiState.audioFeedbackEnabled,
                role = Role.Switch,
                onValueChange = onAudioFeedbackToggled
            )
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_break_reminders)) },
            supportingContent = { Text(stringResource(R.string.settings_break_reminders_desc)) },
            trailingContent = {
                Switch(
                    checked = uiState.breakReminderEnabled,
                    onCheckedChange = null
                )
            },
            modifier = Modifier.toggleable(
                value = uiState.breakReminderEnabled,
                role = Role.Switch,
                onValueChange = onBreakReminderToggled
            )
        )

        AnimatedVisibility(visible = uiState.breakReminderEnabled) {
            val intervalDesc = stringResource(R.string.settings_break_interval_slider)
            var sliderValue by remember { mutableFloatStateOf(uiState.breakReminderIntervalMinutes.toFloat()) }
            LaunchedEffect(uiState.breakReminderIntervalMinutes) {
                sliderValue = uiState.breakReminderIntervalMinutes.toFloat()
            }
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_break_interval, sliderValue.toInt()))
                },
                supportingContent = {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onBreakIntervalChanged(sliderValue.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10,
                        modifier = Modifier.semantics {
                            contentDescription = intervalDesc
                        }
                    )
                }
            )
        }

        HorizontalDivider()

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_setup_guide)) },
            supportingContent = { Text(stringResource(R.string.settings_setup_guide_desc)) },
            modifier = Modifier.clickable(role = Role.Button, onClick = onSetupGuide)
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reopen_onboarding)) },
            supportingContent = { Text(stringResource(R.string.settings_reopen_onboarding_desc)) },
            modifier = Modifier.clickable(role = Role.Button, onClick = onReopenOnboarding)
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_about)) },
            supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
            modifier = Modifier.clickable(role = Role.Button, onClick = onAbout)
        )
    }
}
