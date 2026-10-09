package com.twedmark.app.feature.explorer.data

import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.file.FileNode
import com.twedmark.app.core.file.WorkspaceManager
import com.twedmark.app.core.file.AllowedFormats
import com.twedmark.app.core.file.NameSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/**
 * Repositorio que lee el árbol de archivos del workspace desde disco con Okio,
 * lo aplana según las carpetas expandidas y lo expone como StateFlow<List<FileNode>>.
 *
 * - Lecturas en dispatchers.io.
 * - Orden: carpetas primero, luego archivos; cada grupo alfabético por comparisonKey.
 * - No incluye archivos que empiezan por '.' ni los que terminan en '.tmp'.
 * - Escucha manager.structureRestored y llama a refreshAll().
 * - Máximo 8 niveles bajo workspace/.
 */
class WorkspaceRepository(
    private val fs: FileSystem,
    private val manager: WorkspaceManager,
    private val dispatchers: AppDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val expandedFolders = MutableStateFlow<Set<Path>>(setOf(manager.workspaceRoot))
    private val _tree = MutableStateFlow<List<FileNode>>(emptyList())
    val tree: StateFlow<List<FileNode>> = _tree.asStateFlow()

    init {
        scope.launch {
            refreshAll()
        }
        scope.launch {
            manager.structureRestored.collect {
                refreshAll()
            }
        }
    }

    suspend fun refreshAll() {
        manager.ensureStructure()
        withContext(dispatchers.io) {
            val expanded = expandedFolders.value
            val nodes = mutableListOf<FileNode>()
            buildTree(
                path = manager.workspaceRoot,
                depth = 0,
                expanded = expanded,
                nodes = nodes,
                maxDepth = 8
            )
            _tree.value = nodes
        }
    }

    suspend fun toggleExpanded(folder: Path) {
        withContext(dispatchers.io) {
            val current = expandedFolders.value
            val newExpanded = if (folder in current) {
                current - folder
            } else {
                current + folder
            }
            expandedFolders.value = newExpanded
            
            val expanded = expandedFolders.value
            val nodes = mutableListOf<FileNode>()
            buildTree(
                path = manager.workspaceRoot,
                depth = 0,
                expanded = expanded,
                nodes = nodes,
                maxDepth = 8
            )
            _tree.value = nodes
        }
    }

    private fun buildTree(
        path: Path,
        depth: Int,
        expanded: Set<Path>,
        nodes: MutableList<FileNode>,
        maxDepth: Int
    ) {
        val isDirectory = fs.metadataOrNull(path)?.isDirectory ?: false
        val kind = AllowedFormats.kindOf(path, isDirectory)
        val isExpanded = path in expanded
        val isProtected = manager.isProtected(path)

        val children = if (isDirectory) {
            runCatching { fs.list(path) }.getOrDefault(emptyList())
                .filter { child ->
                    val name = child.name
                    !name.startsWith(".") && !name.endsWith(".tmp")
                }
                .map { child ->
                    // Leemos los metadatos una sola vez por hijo antes de ordenar
                    child to (fs.metadataOrNull(child)?.isDirectory ?: false)
                }
                .sortedWith(compareBy(
                    { if (it.second) 0 else 1 },
                    { NameSanitizer.comparisonKey(it.first.name) }
                ))
                .map { it.first }
        } else {
            emptyList()
        }

        val node = FileNode(
            path = path,
            name = if (path == manager.workspaceRoot) "workspace" else path.name,
            depth = depth,
            kind = kind,
            isExpanded = isExpanded,
            isProtected = isProtected,
            sizeBytes = if (isDirectory) 0L else (fs.metadataOrNull(path)?.size ?: 0L),
            childCount = children.size
        )
        nodes.add(node)

        if (isDirectory && isExpanded && depth < maxDepth) {
            children.forEach { child ->
                buildTree(
                    path = child,
                    depth = depth + 1,
                    expanded = expanded,
                    nodes = nodes,
                    maxDepth = maxDepth
                )
            }
        }
    }
}