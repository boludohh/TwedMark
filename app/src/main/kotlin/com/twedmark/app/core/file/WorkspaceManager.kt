package com.twedmark.app.core.file

import com.twedmark.app.core.common.Outcome
import com.twedmark.app.core.common.AppError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okio.FileSystem
import okio.Path
import java.util.concurrent.atomic.AtomicBoolean

class WorkspaceManager(private val fs: FileSystem, baseDir: Path) {
    val workspaceRoot: Path = baseDir / "workspace"
    val assetsDir: Path = workspaceRoot / "assets"
    val internalDir: Path = baseDir / ".twedmark"
    val crashDir: Path = internalDir / "crash"
    val trashDir: Path = internalDir / "trash"
    val snapshotsDir: Path = internalDir / "snapshots"
    
    private val _structureRestored = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val structureRestored: SharedFlow<Unit> = _structureRestored.asSharedFlow()
    
    private val tmpCleanupDone = AtomicBoolean(false)
    
    fun ensureStructure() {
        var created = false
        if (!fs.exists(workspaceRoot)) {
            fs.createDirectories(workspaceRoot)
            created = true
        }
        if (!fs.exists(assetsDir)) {
            fs.createDirectories(assetsDir)
            created = true
        }
        if (!fs.exists(internalDir)) {
            fs.createDirectories(internalDir)
        }
        if (!fs.exists(crashDir)) {
            fs.createDirectories(crashDir)
        }
        if (!fs.exists(trashDir)) {
            fs.createDirectories(trashDir)
        }
        if (!fs.exists(snapshotsDir)) {
            fs.createDirectories(snapshotsDir)
        }
        
        // Al arrancar también elimina los *.tmp huérfanos del workspace
        // (restos de una escritura interrumpida). Solo se ejecuta una vez.
        if (tmpCleanupDone.compareAndSet(false, true) && fs.exists(workspaceRoot)) {
            runCatching {
                cleanOrphanTmpFiles(workspaceRoot)
            }
        }
        
        if (created) {
            _structureRestored.tryEmit(Unit)
        }
    }
    
    fun isProtected(path: Path): Boolean {
        return path == workspaceRoot || path == assetsDir
    }
    
    fun requireInsideWorkspace(path: Path): Outcome<Path> {
        val normalized = path.normalized()
        val root = workspaceRoot.normalized()
        return if (normalized.toString().startsWith(root.toString())) {
            Outcome.Success(normalized)
        } else {
            Outcome.Failure(AppError.OutsideWorkspace)
        }
    }
    
    fun checkMutable(path: Path): Outcome<Unit> {
        return if (isProtected(path)) {
            Outcome.Failure(AppError.ProtectedPath)
        } else {
            Outcome.Success(Unit)
        }
    }
    
    private fun cleanOrphanTmpFiles(dir: Path) {
        val children = runCatching { fs.list(dir) }.getOrDefault(emptyList())
        for (child in children) {
            val metadata = fs.metadataOrNull(child)
            if (metadata?.isDirectory == true) {
                cleanOrphanTmpFiles(child)
            } else if (child.name.endsWith(".tmp")) {
                runCatching { fs.delete(child) }
            }
        }
    }
}