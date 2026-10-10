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
import androidx.compose.runtime.Composable
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.tree, key = { it.path.toString() }) { node ->
                ExplorerNodeItem(
                    node = node,
                    isSelected = node.path == uiState.selectedFolder,
                    onTap = { onNodeTap(node) },
                    onLongPress = { onNodeLongPress(node) }
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
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val indent = (node.depth * 24).dp
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .then(
                if (isSelected) {
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