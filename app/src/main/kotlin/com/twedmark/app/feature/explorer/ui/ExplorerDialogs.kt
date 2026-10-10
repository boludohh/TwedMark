package com.twedmark.app.feature.explorer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twedmark.app.R
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
                onConfirm = onCreateNote,
                onDismiss = onDismiss
            )
        }
        is ExplorerDialog.CreateFolder -> {
            CreateDialog(
                title = stringResource(R.string.explorer_create_folder_title),
                initialName = "",
                extension = null,
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
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<NameError?>(null) }
    
    LaunchedEffect(name) {
        error = NameSanitizer.validate(name)
    }
    
    val canConfirm = name.isNotEmpty() && error == null

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
                    isError = error != null,
                    supportingText = {
                        when (error) {
                            is NameError.Empty -> Text(stringResource(R.string.error_name_empty))
                            is NameError.DisallowedChar -> Text(
                                stringResource(
                                    R.string.error_name_disallowed_char,
                                    (error as NameError.DisallowedChar).ch
                                )
                            )
                            is NameError.TooLong -> Text(stringResource(R.string.error_name_too_long))
                            is NameError.Duplicate -> Text(stringResource(R.string.error_name_duplicate))
                            null -> {}
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
        // No permitir mover a la misma carpeta actual
        folder != node.path.parent &&
        // Si es carpeta, no permitir mover dentro de sí misma o descendientes
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