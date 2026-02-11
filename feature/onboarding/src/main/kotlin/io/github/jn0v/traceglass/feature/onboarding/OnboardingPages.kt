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
                            SetupTier.FULL_DIY -> "Full DIY — Print your own markers"
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
fun MarkerPreparationPage(
    selectedTier: SetupTier,
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
            text = "Prepare Your Markers",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (selectedTier) {
                SetupTier.FULL_DIY ->
                    "Print two ArUco markers (ID #0 and #1) at about 2 cm each. Tape them at the top-left and top-right corners of your drawing area, so your hands never cover them. Prop your phone about 25\u201330 cm above the surface so the camera sees both markers."
                SetupTier.SEMI_EQUIPPED ->
                    "Print the ArUco marker sheet from the setup guide. Cut out markers #0 and #1, tape them at the top-left and top-right corners of your drawing area. Position your phone about 25\u201330 cm above so both markers stay visible while you draw."
                SetupTier.FULL_KIT ->
                    "Print the ArUco marker sheet. Tape markers #0 and #1 at the top-left and top-right corners. Mount your phone on the stand about 25\u201330 cm above the surface \u2014 high enough to see both markers, low enough to trace comfortably."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onViewGuide) {
            Text("View detailed setup guide")
        }
    }
}
