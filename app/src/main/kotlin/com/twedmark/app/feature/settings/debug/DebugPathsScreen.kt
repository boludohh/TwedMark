package com.twedmark.app.feature.settings.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twedmark.app.R
import com.twedmark.app.ui.theme.TwedMarkTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPathsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugPathsViewModel = koinViewModel()
) {
    val states by viewModel.states.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_paths_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.debug_paths_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::runAll,
                        enabled = !isRunning
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.debug_paths_run_all)
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
        ) {
            DebugSummary(
                passed = viewModel.passedCount,
                failed = viewModel.failedCount,
                total = viewModel.totalCount,
                isRunning = isRunning
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(states.entries.toList(), key = { it.key }) { (name, state) ->
                    DebugCaseItem(name = name, state = state)
                }
            }
        }
    }
}

@Composable
private fun DebugSummary(
    passed: Int,
    failed: Int,
    total: Int,
    isRunning: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_paths_summary),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.debug_paths_passed, passed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0f).let {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = stringResource(R.string.debug_paths_failed, failed),
                style = MaterialTheme.typography.bodyMedium,
                color = if (failed > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.debug_paths_total, total),
                style = MaterialTheme.typography.bodyMedium
            )
            if (isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DebugCaseItem(
    name: String,
    state: DebugPathsViewModel.CaseState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                is DebugPathsViewModel.CaseState.Pending -> {
                    Text("○", modifier = Modifier.padding(end = 8.dp))
                }
                is DebugPathsViewModel.CaseState.Running -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                is DebugPathsViewModel.CaseState.Passed -> {
                    Text(
                        "✓",
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is DebugPathsViewModel.CaseState.Failed -> {
                    Text(
                        "✗",
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state is DebugPathsViewModel.CaseState.Failed) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}