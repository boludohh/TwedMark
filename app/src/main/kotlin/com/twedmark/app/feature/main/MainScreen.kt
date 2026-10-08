package com.twedmark.app.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twedmark.app.BuildConfig
import com.twedmark.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToCrashLog: () -> Unit,
    onNavigateToDebugPaths: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(R.string.main_welcome),
                style = MaterialTheme.typography.headlineMedium
            )

            Button(onClick = onNavigateToCrashLog) {
                Text(stringResource(R.string.main_view_crash_logs))
            }

            if (BuildConfig.DEBUG) {
                Button(onClick = onNavigateToDebugPaths) {
                    Text(stringResource(R.string.main_view_debug_paths))
                }
            }
        }
    }
}