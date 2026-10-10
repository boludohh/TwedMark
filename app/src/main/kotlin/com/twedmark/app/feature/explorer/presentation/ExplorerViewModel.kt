package com.twedmark.app.feature.explorer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.Outcome
import com.twedmark.app.core.file.FileNode
import com.twedmark.app.core.file.NodeKind
import com.twedmark.app.feature.explorer.data.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path

sealed interface ExplorerDialog {
    data class CreateNote(val parent: Path) : ExplorerDialog
    data class CreateFolder(val parent: Path) : ExplorerDialog
    data class Rename(val node: FileNode) : ExplorerDialog
    data class Move(val node: FileNode, val folders: List<Path>) : ExplorerDialog
    data class ConfirmDelete(val node: FileNode, val descendants: Int) : ExplorerDialog
}

sealed interface ExplorerEffect {
    data class Message(val error: AppError) : ExplorerEffect
    data class Info(val message: String) : ExplorerEffect
    data class OpenNote(val path: Path) : ExplorerEffect
}

data class ExplorerUiState(
    val tree: List<FileNode> = emptyList(),
    val selectedFolder: Path? = null,
    val dialog: ExplorerDialog? = null,
    val isLoading: Boolean = false
)

class ExplorerViewModel(private val repository: WorkspaceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<ExplorerEffect?>(null)
    val effects: StateFlow<ExplorerEffect?> = _effects.asStateFlow()

    init {
        viewModelScope.launch {
            repository.tree.collect { tree ->
                _uiState.update { it.copy(tree = tree) }
            }
        }
    }

    fun clearEffect() {
        _effects.value = null
    }

    fun toggleExpanded(node: FileNode) {
        if (node.kind == NodeKind.Folder) {
            viewModelScope.launch {
                repository.toggleExpanded(node.path)
            }
        }
    }

    fun selectFolder(node: FileNode) {
        if (node.kind == NodeKind.Folder) {
            _uiState.update { it.copy(selectedFolder = node.path) }
        } else {
            _uiState.update { it.copy(selectedFolder = node.path.parent) }
        }
    }

    fun onNodeTap(node: FileNode) {
        when (node.kind) {
            NodeKind.Folder -> {
                toggleExpanded(node)
                selectFolder(node)
            }
            NodeKind.Note -> {
                _effects.value = ExplorerEffect.OpenNote(node.path)
            }
            NodeKind.Image -> {
                _effects.value = ExplorerEffect.Info("Las imágenes se visualizarán en la Fase 2")
            }
            NodeKind.Unsupported -> {
                _effects.value = ExplorerEffect.Info("Formato no soportado")
            }
        }
    }

    fun onNodeLongPress(node: FileNode) {
        if (node.isProtected) return
        
        viewModelScope.launch {
            val descendants = if (node.kind == NodeKind.Folder) {
                repository.countDescendants(node.path)
            } else 0
            
            val folders = repository.listFolders()
            
            _uiState.update { 
                it.copy(
                    dialog = ExplorerDialog.ConfirmDelete(node, descendants)
                )
            }
        }
    }

    fun showCreateNoteDialog() {
        val parent = _uiState.value.selectedFolder ?: return
        _uiState.update { it.copy(dialog = ExplorerDialog.CreateNote(parent)) }
    }

    fun showCreateFolderDialog() {
        val parent = _uiState.value.selectedFolder ?: return
        _uiState.update { it.copy(dialog = ExplorerDialog.CreateFolder(parent)) }
    }

    fun showRenameDialog(node: FileNode) {
        _uiState.update { it.copy(dialog = ExplorerDialog.Rename(node)) }
    }

    fun showMoveDialog(node: FileNode) {
        viewModelScope.launch {
            val folders = repository.listFolders()
            _uiState.update { it.copy(dialog = ExplorerDialog.Move(node, folders)) }
        }
    }

    fun showDeleteDialog(node: FileNode) {
        viewModelScope.launch {
            val descendants = if (node.kind == NodeKind.Folder) {
                repository.countDescendants(node.path)
            } else 0
            _uiState.update { it.copy(dialog = ExplorerDialog.ConfirmDelete(node, descendants)) }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = null) }
    }

    fun createNote(baseName: String) {
        val dialog = _uiState.value.dialog as? ExplorerDialog.CreateNote ?: return
        viewModelScope.launch {
            val result = repository.createNote(dialog.parent, baseName)
            when (result) {
                is Outcome.Success -> {
                    dismissDialog()
                    _effects.value = ExplorerEffect.OpenNote(result.value)
                }
                is Outcome.Failure -> {
                    _effects.value = ExplorerEffect.Message(result.error)
                }
            }
        }
    }

    fun createFolder(baseName: String) {
        val dialog = _uiState.value.dialog as? ExplorerDialog.CreateFolder ?: return
        viewModelScope.launch {
            val result = repository.createFolder(dialog.parent, baseName)
            when (result) {
                is Outcome.Success -> {
                    dismissDialog()
                }
                is Outcome.Failure -> {
                    _effects.value = ExplorerEffect.Message(result.error)
                }
            }
        }
    }

    fun rename(baseName: String) {
        val dialog = _uiState.value.dialog as? ExplorerDialog.Rename ?: return
        viewModelScope.launch {
            val result = repository.rename(dialog.node.path, baseName)
            when (result) {
                is Outcome.Success -> {
                    dismissDialog()
                }
                is Outcome.Failure -> {
                    _effects.value = ExplorerEffect.Message(result.error)
                }
            }
        }
    }

    fun move(targetFolder: Path) {
        val dialog = _uiState.value.dialog as? ExplorerDialog.Move ?: return
        viewModelScope.launch {
            val result = repository.move(dialog.node.path, targetFolder)
            when (result) {
                is Outcome.Success -> {
                    dismissDialog()
                }
                is Outcome.Failure -> {
                    _effects.value = ExplorerEffect.Message(result.error)
                }
            }
        }
    }

    fun delete() {
        val dialog = _uiState.value.dialog as? ExplorerDialog.ConfirmDelete ?: return
        viewModelScope.launch {
            val result = repository.delete(dialog.node.path)
            when (result) {
                is Outcome.Success -> {
                    dismissDialog()
                }
                is Outcome.Failure -> {
                    _effects.value = ExplorerEffect.Message(result.error)
                }
            }
        }
    }
}