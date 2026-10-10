package com.twedmark.app.feature.explorer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twedmark.app.R
import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.file.FileNode
import com.twedmark.app.core.file.NameError
import com.twedmark.app.core.file.NameSanitizer
import com.twedmark.app.feature.explorer.presentation.ExplorerDialog
import okio.Path

@Composable
fun ExplorerDialogHost(
    dialog: ExplorerDialog?,
    onDismiss: () -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRename: (String) -> Unit,
    onMove: (Path) -> Unit,
    onDelete: () -> Unit
) {
    when (dialog) {
        is ExplorerDialog.CreateNote -> {
            CreateDialog(
                title = stringResource(R.string.explorer_create_note_title),
                initialName = "",
                extension = ".md",
                repositoryError = dialog.error,
                onConfirm = onCreateNote,
                onDismiss = onDismiss
            )
        }
        is ExplorerDialog.CreateFolder -> {
            CreateDialog(
                title = stringResource(R.string.explorer_create_folder_title),
                initialName = "",
                extension = null,
                repositoryError = dialog.error,
                onConfirm = onCreateFolder,
                onDismiss = onDismiss
            )
        }
        is ExplorerDialog.Rename -> {
            val baseName = dialog.node.name.substringBeforeLast('.', dialog.node.name)
            val extension = if (dialog.node.name.contains('.')) {
                "." + dialog.node.name.substringAfterLast('.')
            } else null
            
            CreateDialog(
                title = stringResource(R.string.explorer_rename_title),
                initialName = baseName,
                extension = extension,
                repositoryError = dialog.error,
                onConfirm = onRename,
                onDismiss = onDismiss
            )
        }
        is ExplorerDialog.Move -> {
            MoveDialog(
                node = dialog.node,
                folders = dialog.folders,
                onConfirm = onMove,
                onDismiss = onDismiss
            )
        }
        is ExplorerDialog.ConfirmDelete -> {
            DeleteDialog(
                node = dialog.node,
                descendants = dialog.descendants,
                onConfirm = onDelete,
                onDismiss = onDismiss
            )
        }
        null -> {}
    }
}

@Composable
private fun CreateDialog(
    title: String,
    initialName: String,
    extension: String?,
    repositoryError: AppError?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var validationError by remember { mutableStateOf<NameError?>(null) }
    
    LaunchedEffect(name) {
        validationError = NameSanitizer.validate(name)
    }
    
    // Si el nombre cambia, limpiar el error del repositorio
    LaunchedEffect(name) {
        // El error del repositorio se limpiará cuando el ViewModel recree el diálogo sin error
    }
    
    val hasValidationError = validationError != null
    val hasRepositoryError = repositoryError != null
    val canConfirm = name.isNotEmpty() && !hasValidationError && !hasRepositoryError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.explorer_name_label)) },
                    singleLine = true,
                    isError = hasValidationError || hasRepositoryError,
                    supportingText = {
                        when {
                            validationError is NameError.Empty -> {
                                Text(stringResource(R.string.error_name_empty))
                            }
                            validationError is NameError.DisallowedChar -> {
                                val char = (validationError as NameError.DisallowedChar).ch
                                if (char == " ") {
                                    Text(stringResource(R.string.error_name_space))
                                } else {
                                    Text(stringResource(R.string.error_name_disallowed_char, char))
                                }
                            }
                            validationError is NameError.TooLong -> {
                                Text(stringResource(R.string.error_name_too_long))
                            }
                            validationError is NameError.Duplicate -> {
                                Text(stringResource(R.string.error_name_duplicate))
                            }
                            repositoryError is AppError.AlreadyExists -> {
                                Text(
                                    text = stringResource(R.string.error_name_duplicate),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            repositoryError != null -> {
                                Text(
                                    text = repositoryError.toString(),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (extension != null) {
                            Text(
                                text = extension,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = canConfirm
            ) {
                Text(stringResource(R.string.action_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun MoveDialog(
    node: FileNode,
    folders: List<Path>,
    onConfirm: (Path) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFolder by remember { mutableStateOf<Path?>(null) }
    
    val validFolders = folders.filter { folder ->
        folder != node.path.parent &&
        !(node.kind == com.twedmark.app.core.file.NodeKind.Folder && 
          folder.toString().startsWith(node.path.toString()))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.explorer_move_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.explorer_move_message, node.name),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(validFolders) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFolder == folder,
                                onClick = { selectedFolder = folder }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folder.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedFolder?.let { onConfirm(it) } },
                enabled = selectedFolder != null
            ) {
                Text(stringResource(R.string.action_move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DeleteDialog(
    node: FileNode,
    descendants: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = if (descendants > 0) {
        stringResource(R.string.explorer_delete_message_with_descendants, node.name, descendants)
    } else {
        stringResource(R.string.explorer_delete_message, node.name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.explorer_delete_title)) },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.explorer_delete_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}