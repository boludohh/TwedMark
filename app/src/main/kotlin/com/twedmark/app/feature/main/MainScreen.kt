package com.twedmark.app.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.twedmark.app.R
import com.twedmark.app.feature.editor.domain.SaveState
import com.twedmark.app.feature.editor.presentation.EditorEvent
import com.twedmark.app.feature.editor.presentation.EditorViewModel
import com.twedmark.app.feature.editor.presentation.FlushReason
import com.twedmark.app.feature.editor.ui.CodeEditorView
import com.twedmark.app.feature.explorer.presentation.ExplorerDialog
import com.twedmark.app.feature.explorer.presentation.ExplorerEffect
import com.twedmark.app.feature.explorer.presentation.ExplorerViewModel
import com.twedmark.app.feature.explorer.ui.ExplorerDialogHost
import com.twedmark.app.feature.explorer.ui.ExplorerPanel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToCrashLog: () -> Unit,
    onNavigateToDebugPaths: () -> Unit,
    editorViewModel: EditorViewModel = koinViewModel(),
    explorerViewModel: ExplorerViewModel = koinViewModel()
) {
    val editorUiState by editorViewModel.uiState.collectAsState()
    val explorerUiState by explorerViewModel.uiState.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // BackHandler: primero cierra diálogos, luego drawer, luego sale
    BackHandler(enabled = explorerUiState.dialog != null || drawerState.isOpen) {
        when {
            explorerUiState.dialog != null -> explorerViewModel.dismissDialog()
            drawerState.isOpen -> scope.launch { drawerState.close() }
        }
    }

    // Flush al pasar a segundo plano (ON_STOP)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        editorViewModel.sendEvent(EditorEvent.FlushRequested(FlushReason.ScreenStopped))
    }

    // Efectos del explorador
    LaunchedEffect(Unit) {
        explorerViewModel.effects.collect { effect ->
            when (effect) {
                is ExplorerEffect.Message -> {
                    snackbarHostState.showSnackbar(
                        message = "Error: ${effect.error}",
                        duration = SnackbarDuration.Short
                    )
                    explorerViewModel.clearEffect()
                }
                is ExplorerEffect.Info -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                    explorerViewModel.clearEffect()
                }
                is ExplorerEffect.OpenNote -> {
                    editorViewModel.sendEvent(EditorEvent.OpenFile(effect.path))
                    drawerState.close()
                    explorerViewModel.clearEffect()
                }
                null -> {}
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                ExplorerPanel(
                    uiState = explorerUiState,
                    onNodeTap = { node ->
                        explorerViewModel.onNodeTap(node)
                        if (node.kind == com.twedmark.app.core.file.NodeKind.Note) {
                            scope.launch { drawerState.close() }
                        }
                    },
                    onNodeLongPress = explorerViewModel::onNodeLongPress,
                    onCreateNote = explorerViewModel::showCreateNoteDialog,
                    onCreateFolder = explorerViewModel::showCreateFolderDialog
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = editorUiState.document?.displayName ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.action_open_menu),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Indicador de estado de guardado
                        when (editorUiState.saveState) {
                            is SaveState.Saved -> {
                                Text(
                                    text = stringResource(R.string.editor_state_saved),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            is SaveState.Dirty -> {
                                Text(
                                    text = stringResource(R.string.editor_state_dirty),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.warning,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            is SaveState.Saving -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(horizontal = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is SaveState.Failed -> {
                                Text(
                                    text = stringResource(R.string.editor_state_failed),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                        
                        IconButton(onClick = { editorViewModel.sendEvent(EditorEvent.SaveClicked) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_content_copy_outline),
                                contentDescription = stringResource(R.string.action_save),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                if (editorUiState.document != null) {
                    CodeEditorView(
                        modifier = Modifier.fillMaxSize(),
                        onControllerReady = { controller ->
                            editorViewModel.setController(controller)
                        }
                    )
                } else {
                    // Estado vacío: sin documento abierto
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.editor_no_document),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { scope.launch { drawerState.open() } }) {
                            Text(stringResource(R.string.editor_open_explorer))
                        }
                    }
                }
            }
        }
    }

    // Diálogos del explorador
    ExplorerDialogHost(
        dialog = explorerUiState.dialog,
        onDismiss = explorerViewModel::dismissDialog,
        onCreateNote = explorerViewModel::createNote,
        onCreateFolder = explorerViewModel::createFolder,
        onRename = explorerViewModel::rename,
        onMove = explorerViewModel::move,
        onDelete = explorerViewModel::delete
    )
}