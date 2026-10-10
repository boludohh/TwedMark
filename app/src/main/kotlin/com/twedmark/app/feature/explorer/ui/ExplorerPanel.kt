package com.twedmark.app.feature.explorer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twedmark.app.R
import com.twedmark.app.core.file.FileNode
import com.twedmark.app.core.file.NodeKind
import com.twedmark.app.feature.explorer.presentation.ExplorerUiState

@Composable
fun ExplorerPanel(
    uiState: ExplorerUiState,
    onNodeTap: (FileNode) -> Unit,
    onNodeLongPress: (FileNode) -> Unit,
    onDismissContextMenu: () -> Unit,
    onContextMenuRename: () -> Unit,
    onContextMenuMove: () -> Unit,
    onContextMenuDelete: () -> Unit,
    onCreateNote: () -> Unit,
    onCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Barra superior del panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.explorer_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onCreateFolder) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(R.string.explorer_create_folder),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = onCreateNote) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(R.string.explorer_create_note),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Divider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        // Árbol de archivos
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.tree, key = { it.path.toString() }) { node ->
                    ExplorerNodeItem(
                        node = node,
                        isSelected = node.path == uiState.selectedFolder,
                        isContextMenuTarget = node.path == uiState.contextMenuNode?.path,
                        onTap = { onNodeTap(node) },
                        onLongPress = { onNodeLongPress(node) }
                    )
                }
            }

            // Menú contextual anclado al nodo objetivo
            val contextNode = uiState.contextMenuNode
            if (contextNode != null) {
                ContextMenu(
                    node = contextNode,
                    onDismiss = onDismissContextMenu,
                    onRename = onContextMenuRename,
                    onMove = onContextMenuMove,
                    onDelete = onContextMenuDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerNodeItem(
    node: FileNode,
    isSelected: Boolean,
    isContextMenuTarget: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val indent = (node.depth * 24).dp
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .then(
                if (isSelected || isContextMenuTarget) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = if (node.isProtected) null else onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono de expansión para carpetas
        if (node.kind == NodeKind.Folder) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (node.isExpanded) 90f else 0f)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Icono del tipo de archivo
        val iconRes = when (node.kind) {
            NodeKind.Folder -> R.drawable.ic_folder_filled
            NodeKind.Note -> R.drawable.ic_markdown_document_filled
            NodeKind.Image -> R.drawable.ic_image_filled
            NodeKind.Unsupported -> R.drawable.ic_unknown_document_filled
        }
        
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))

        // Nombre del archivo/carpeta
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Menú contextual con opciones: Renombrar, Mover, Eliminar.
 * Se muestra centrado en la pantalla cuando hay un nodo marcado.
 */
@Composable
private fun ContextMenu(
    node: FileNode,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    // Overlay semi-transparente para capturar taps fuera del menú
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .combinedClickable(
                onClick = onDismiss,
                onLongClick = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 320.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Cabecera con el nombre del archivo
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Ítem: Renombrar
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.explorer_context_rename),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = onRename
                )
                
                // Ítem: Mover
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.explorer_context_move),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_move_item),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = onMove
                )
                
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                // Ítem: Eliminar (en rojo)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.explorer_context_delete),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = onDelete
                )
            }
        }
    }
}