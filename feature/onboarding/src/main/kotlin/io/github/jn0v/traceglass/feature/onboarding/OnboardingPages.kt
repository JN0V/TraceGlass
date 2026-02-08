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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
            text = "Welcome to TraceGlass",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Turn your phone into a tracing lightbox. Point your camera at your drawing surface, pick a reference image, and trace with precision.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TierSelectionPage(
    selectedTier: SetupTier,
    onTierSelected: (SetupTier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose Your Setup",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select the option that matches your equipment.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        SetupTier.entries.forEach { tier ->
            FilterChip(
                selected = tier == selectedTier,
                onClick = { onTierSelected(tier) },
                label = {
                    Text(
                        when (tier) {
                            SetupTier.FULL_DIY -> "Full DIY — Draw your own markers"
                            SetupTier.SEMI_EQUIPPED -> "Semi-equipped — Printed markers"
                            SetupTier.FULL_KIT -> "Full kit — Printed markers + stand"
                        }
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MarkerPreparationPage(selectedTier: SetupTier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Prepare Your Markers",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (selectedTier) {
                SetupTier.FULL_DIY ->
                    "Draw two distinct markers on paper (e.g., a heart and a star). Place them 10-15 cm apart on your drawing surface. The app will detect them to stabilize your overlay."
                SetupTier.SEMI_EQUIPPED ->
                    "Print the ArUco marker sheet from the setup guide. Cut out markers #0 and #1, and place them 10-15 cm apart on your drawing surface."
                SetupTier.FULL_KIT ->
                    "Print the ArUco marker sheet and set up your phone stand. Place markers #0 and #1 about 10-15 cm apart, then mount your phone above the surface."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
