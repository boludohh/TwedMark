package com.twedmark.app.feature.explorer.data

import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.Outcome
import com.twedmark.app.core.file.AllowedFormats
import com.twedmark.app.core.file.AtomicFileWriter
import com.twedmark.app.core.file.FileNode
import com.twedmark.app.core.file.FsEvent
import com.twedmark.app.core.file.LineEnding
import com.twedmark.app.core.file.NameSanitizer
import com.twedmark.app.core.file.NodeKind
import com.twedmark.app.core.file.NoteContent
import com.twedmark.app.core.file.NoteIO
import com.twedmark.app.core.file.WorkspaceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/**
 * Repositorio que gestiona el árbol de archivos del workspace.
 * 
 * - Expone el árbol aplanado como StateFlow<List<FileNode>>.
 * - Emite eventos FsEvent cuando el sistema de archivos cambia (Fase 1).
 * - Ejecuta todas las operaciones de I/O en dispatchers.io.
 * - Aplica saneamiento de nombres, control de profundidad (máx 8) y rutas protegidas.
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

    // Instancias internas de I/O para evitar modificar el módulo de Koin
    private val atomicWriter = AtomicFileWriter(fs)
    private val noteIO = NoteIO(fs, atomicWriter)

    // Canal de eventos para notificar cambios en el sistema de archivos a otros componentes (ej. Editor).
    private val _events = MutableSharedFlow<FsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<FsEvent> = _events.asSharedFlow()

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

    // --- Extensiones y Utilidades ---

    private fun Path.depthFromRoot(): Int {
        return this.normalized().segments.size - manager.workspaceRoot.normalized().segments.size
    }

    private fun getMaxDepth(path: Path): Int {
        val isDir = fs.metadataOrNull(path)?.isDirectory ?: false
        val currentDepth = path.depthFromRoot()
        if (!isDir) return currentDepth
        
        var max = currentDepth
        val children = runCatching { fs.list(path) }.getOrDefault(emptyList())
        for (child in children) {
            val childDepth = getMaxDepth(child)
            if (childDepth > max) max = childDepth
        }
        return max
    }

    // --- Operaciones del Explorador (CRUD) ---

    suspend fun createFolder(parent: Path, rawName: String): Outcome<Path> {
        val nameError = NameSanitizer.validate(rawName)
        if (nameError != null) return Outcome.Failure(AppError.InvalidName(nameError))
        
        val checkParent = manager.checkMutable(parent)
        if (checkParent is Outcome.Failure) return checkParent
        
        val target = parent / rawName
        val inside = manager.requireInsideWorkspace(target)
        if (inside is Outcome.Failure) return inside
        
        return withContext(dispatchers.io) {
            val parentDepth = parent.depthFromRoot()
            if ((parentDepth + 1) > 8) return@withContext Outcome.Failure(AppError.DepthLimitExceeded)
            
            if (fs.exists(target)) return@withContext Outcome.Failure(AppError.AlreadyExists)
            
            try {
                fs.createDirectory(target)
            } catch (e: Exception) {
                return@withContext Outcome.Failure(AppError.WriteFailed(e))
            }
            
            _events.tryEmit(FsEvent.FileCreated(target))
            refreshAll()
            Outcome.Success(target)
        }
    }

    suspend fun createNote(parent: Path, rawName: String, initialContent: String = ""): Outcome<Path> {
        val nameError = NameSanitizer.validate(rawName)
        if (nameError != null) return Outcome.Failure(AppError.InvalidName(nameError))
        
        val checkParent = manager.checkMutable(parent)
        if (checkParent is Outcome.Failure) return checkParent
        
        val target = parent / "$rawName.md"
        val inside = manager.requireInsideWorkspace(target)
        if (inside is Outcome.Failure) return inside
        
        return withContext(dispatchers.io) {
            val parentDepth = parent.depthFromRoot()
            if ((parentDepth + 1) > 8) return@withContext Outcome.Failure(AppError.DepthLimitExceeded)
            
            if (fs.exists(target)) return@withContext Outcome.Failure(AppError.AlreadyExists)
            
            val content = NoteContent(text = initialContent, hadBom = false, lineEnding = LineEnding.LF)
            val writeResult = noteIO.write(target, content)
            if (writeResult is Outcome.Failure) return@withContext writeResult
            
            _events.tryEmit(FsEvent.FileCreated(target))
            refreshAll()
            Outcome.Success(target)
        }
    }

    suspend fun rename(path: Path, rawNewName: String): Outcome<Path> {
        val nameError = NameSanitizer.validate(rawNewName)
        if (nameError != null) return Outcome.Failure(AppError.InvalidName(nameError))
        
        val check = manager.checkMutable(path)
        if (check is Outcome.Failure) return check
        
        val inside = manager.requireInsideWorkspace(path)
        if (inside is Outcome.Failure) return inside
        
        val parent = path.parent ?: return Outcome.Failure(AppError.OutsideWorkspace)
        
        val isDir = fs.metadataOrNull(path)?.isDirectory ?: false
        val newName = if (isDir) {
            rawNewName
        } else {
            val ext = path.name.substringAfterLast('.', "")
            if (ext.isNotEmpty() && ext.lowercase() in (AllowedFormats.noteExtensions + AllowedFormats.imageExtensions)) {
                "$rawNewName.$ext"
            } else {
                rawNewName
            }
        }
        
        val target = parent / newName
        val insideTarget = manager.requireInsideWorkspace(target)
        if (insideTarget is Outcome.Failure) return insideTarget
        
        return withContext(dispatchers.io) {
            if (!fs.exists(path)) return@withContext Outcome.Failure(AppError.NotFound)
            if (target != path && fs.exists(target)) return@withContext Outcome.Failure(AppError.AlreadyExists)
            if (target == path) return@withContext Outcome.Success(target)
            
            try {
                fs.atomicMove(path, target)
            } catch (e: Exception) {
                return@withContext Outcome.Failure(AppError.WriteFailed(e))
            }
            
            _events.tryEmit(FsEvent.FileRenamed(path, target))
            refreshAll()
            Outcome.Success(target)
        }
    }

    suspend fun move(source: Path, targetParent: Path): Outcome<Path> {
        val checkSource = manager.checkMutable(source)
        if (checkSource is Outcome.Failure) return checkSource
        
        val insideSource = manager.requireInsideWorkspace(source)
        if (insideSource is Outcome.Failure) return insideSource
        
        val insideTarget = manager.requireInsideWorkspace(targetParent)
        if (insideTarget is Outcome.Failure) return insideTarget
        
        return withContext(dispatchers.io) {
            if (!fs.exists(source)) return@withContext Outcome.Failure(AppError.NotFound)
            
            val isDir = fs.metadataOrNull(source)?.isDirectory ?: false
            
            // MoveIntoItself check
            if (isDir) {
                val sourceSegments = source.normalized().segments
                val targetSegments = targetParent.normalized().segments
                if (targetSegments.size >= sourceSegments.size) {
                    var isInside = true
                    for (i in sourceSegments.indices) {
                        if (sourceSegments[i] != targetSegments[i]) {
                            isInside = false
                            break
                        }
                    }
                    if (isInside) return@withContext Outcome.Failure(AppError.MoveIntoItself)
                }
            }
            
            // DepthLimitExceeded check
            val targetDepth = targetParent.depthFromRoot()
            if (isDir) {
                val maxChildDepth = getMaxDepth(source)
                val sourceDepth = source.depthFromRoot()
                val relativeDepth = maxChildDepth - sourceDepth
                if ((targetDepth + 1 + relativeDepth) > 8) {
                    return@withContext Outcome.Failure(AppError.DepthLimitExceeded)
                }
            } else {
                if ((targetDepth + 1) > 8) {
                    return@withContext Outcome.Failure(AppError.DepthLimitExceeded)
                }
            }
            
            val target = targetParent / source.name
            if (fs.exists(target)) return@withContext Outcome.Failure(AppError.AlreadyExists)
            
            try {
                fs.atomicMove(source, target)
            } catch (e: Exception) {
                return@withContext Outcome.Failure(AppError.WriteFailed(e))
            }
            
            _events.tryEmit(FsEvent.FileMoved(source, target))
            refreshAll()
            Outcome.Success(target)
        }
    }

    suspend fun delete(path: Path): Outcome<Unit> {
        val check = manager.checkMutable(path)
        if (check is Outcome.Failure) return check
        
        val inside = manager.requireInsideWorkspace(path)
        if (inside is Outcome.Failure) return inside
        
        return withContext(dispatchers.io) {
            if (!fs.exists(path)) return@withContext Outcome.Failure(AppError.NotFound)
            
            try {
                fs.deleteRecursively(path)
            } catch (e: Exception) {
                return@withContext Outcome.Failure(AppError.WriteFailed(e))
            }
            
            _events.tryEmit(FsEvent.FileDeleted(path))
            refreshAll()
            Outcome.Success(Unit)
        }
    }

    // --- Operaciones de Lectura y Estado ---

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