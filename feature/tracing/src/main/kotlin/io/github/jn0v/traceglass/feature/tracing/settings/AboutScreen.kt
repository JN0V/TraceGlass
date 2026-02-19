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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jn0v.traceglass.feature.tracing.R

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
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back)
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
                    Text(stringResource(R.string.about_app_name), style = MaterialTheme.typography.headlineSmall)
                },
                supportingContent = {
                    Text(stringResource(R.string.about_version, versionName, versionCode))
                }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.about_license)) },
                supportingContent = { Text(stringResource(R.string.about_license_value)) }
            )

            HorizontalDivider()

            ListItem(
                overlineContent = { Text(stringResource(R.string.about_open_source)) },
                headlineContent = { Text(stringResource(R.string.about_third_party)) },
                supportingContent = {
                    Text(stringResource(R.string.about_third_party_list))
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
                Text(stringResource(R.string.about_view_github))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
