package io.github.jn0v.traceglass.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jn0v.traceglass.core.cv.DetectionMode

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TierSelectionPage(
    selectedTier: SetupTier,
    onTierSelected: (SetupTier) -> Unit,
    detectionMode: DetectionMode = DetectionMode.ARUCO,
    onDetectionModeSelected: (DetectionMode) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_tier_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_tier_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.semantics { selectableGroup() }) {
            SetupTier.entries.forEach { tier ->
                val label = tierLabel(tier)
                FilterChip(
                    selected = tier == selectedTier,
                    onClick = { onTierSelected(tier) },
                    label = { Text(label) },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .semantics {
                            contentDescription = if (tier == selectedTier) {
                                "$label, selected"
                            } else {
                                label
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_mode_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Column(modifier = Modifier.semantics { selectableGroup() }) {
            DetectionMode.entries.forEach { mode ->
                val label = detectionModeLabel(mode)
                FilterChip(
                    selected = mode == detectionMode,
                    onClick = { onDetectionModeSelected(mode) },
                    label = { Text(label) },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .semantics {
                            contentDescription = if (mode == detectionMode) {
                                "$label, selected"
                            } else {
                                label
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun MarkerPreparationPage(
    selectedTier: SetupTier,
    detectionMode: DetectionMode = DetectionMode.ARUCO,
    onViewGuide: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_markers_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(
                if (detectionMode == DetectionMode.CUSTOM_SHAPES) {
                    R.string.onboarding_markers_custom_shapes
                } else {
                    markerInstructionsRes(selectedTier)
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onViewGuide) {
            Text(stringResource(R.string.onboarding_view_guide))
        }
    }
}

@Composable
private fun tierLabel(tier: SetupTier): String = stringResource(
    when (tier) {
        SetupTier.FULL_DIY -> R.string.onboarding_tier_full_diy
        SetupTier.SEMI_EQUIPPED -> R.string.onboarding_tier_semi_equipped
        SetupTier.FULL_KIT -> R.string.onboarding_tier_full_kit
    }
)

@Composable
private fun detectionModeLabel(mode: DetectionMode): String = stringResource(
    when (mode) {
        DetectionMode.ARUCO -> R.string.onboarding_mode_aruco
        DetectionMode.CUSTOM_SHAPES -> R.string.onboarding_mode_custom_shapes
    }
)

private fun markerInstructionsRes(tier: SetupTier): Int = when (tier) {
    SetupTier.FULL_DIY -> R.string.onboarding_markers_full_diy
    SetupTier.SEMI_EQUIPPED -> R.string.onboarding_markers_semi_equipped
    SetupTier.FULL_KIT -> R.string.onboarding_markers_full_kit
}

@Preview(showBackground = true)
@Composable
private fun WelcomePagePreview() {
    MaterialTheme {
        WelcomePage()
    }
}

@Preview(showBackground = true)
@Composable
private fun TierSelectionPagePreview() {
    MaterialTheme {
        TierSelectionPage(
            selectedTier = SetupTier.FULL_DIY,
            onTierSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MarkerPreparationPagePreview() {
    MaterialTheme {
        MarkerPreparationPage(
            selectedTier = SetupTier.FULL_DIY,
            onViewGuide = {}
        )
    }
}
