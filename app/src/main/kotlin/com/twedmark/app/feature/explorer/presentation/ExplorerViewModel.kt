package com.twedmark.app.feature.explorer.presentation

import androidx.lifecycle.ViewModel
import com.twedmark.app.core.file.FileNode
import com.twedmark.app.feature.explorer.data.WorkspaceRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel del explorador de archivos.
 * Por ahora solo expone el árbol de archivos del repositorio.
 * Las acciones de crear, renombrar, mover, borrar se añadirán en fases posteriores.
 */
class ExplorerViewModel(private val repository: WorkspaceRepository) : ViewModel() {

    val tree: StateFlow<List<FileNode>> = repository.tree

    suspend fun toggleExpanded(node: FileNode) {
        if (node.kind == com.twedmark.app.core.file.NodeKind.Folder) {
            repository.toggleExpanded(node.path)
        }
    }
}