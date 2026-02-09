package io.github.jn0v.traceglass.feature.tracing.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    versionCode: Int,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = {
                    Text("TraceGlass", style = MaterialTheme.typography.headlineSmall)
                },
                supportingContent = {
                    Text("Version $versionName (build $versionCode)")
                }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text("License") },
                supportingContent = { Text("GNU General Public License v3.0") }
            )

            HorizontalDivider()

            ListItem(
                overlineContent = { Text("Open-source libraries") },
                headlineContent = { Text("Third-party licenses") },
                supportingContent = {
                    Text(
                        "OpenCV (Apache 2.0)\n" +
                            "Coil (Apache 2.0)\n" +
                            "Koin (Apache 2.0)\n" +
                            "Jetpack libraries (Apache 2.0)"
                    )
                }
            )

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    uriHandler.openUri("https://github.com/jn0v/traceglass")
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("View on GitHub (opens browser)")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
