package com.twedmark.app.feature.editor.domain

import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.Outcome
import com.twedmark.app.core.file.AtomicFileWriter
import com.twedmark.app.core.file.LineEnding
import com.twedmark.app.core.file.NoteContent
import com.twedmark.app.core.file.NoteIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/**
 * Estados posibles del guardado de un documento.
 */
sealed interface SaveState {
    /** El texto en memoria es idéntico al último texto guardado en disco. */
    data object Saved : SaveState
    
    /** El texto en memoria ha cambiado pero aún no se ha guardado. */
    data object Dirty : SaveState
    
    /** Hay una operación de escritura en curso. */
    data object Saving : SaveState
    
    /** La última operación de guardado falló. */
    data class Failed(val error: AppError) : SaveState
}

/**
 * Gestor de guardado seguro para el editor.
 * 
 * - Serializa las escrituras mediante un [Mutex].
 * - Mantiene el estado de guardado como [StateFlow].
 * - Implementa idempotencia: no escribe a disco si el texto no ha cambiado.
 * - Preserva el BOM y el salto de línea original del archivo.
 * - Soporta [remap] para actualizar la ruta si el archivo se renombra/mueve externamente.
 */
class DocumentSaver(
    fs: FileSystem,
    private val dispatchers: AppDispatchers
) {
    private val atomicWriter = AtomicFileWriter(fs)
    private val noteIO = NoteIO(fs, atomicWriter)
    
    private val mutex = Mutex()
    
    private var currentPath: Path? = null
    private var lastSavedText: String? = null
    private var lastHadBom: Boolean = false
    private var lastLineEnding: LineEnding = LineEnding.LF
    
    private val _state = MutableStateFlow<SaveState>(SaveState.Saved)
    val state: StateFlow<SaveState> = _state.asStateFlow()
    
    /**
     * Abre un documento y establece su estado inicial como [SaveState.Saved].
     */
    fun open(path: Path, text: String, hadBom: Boolean, lineEnding: LineEnding) {
        currentPath = path
        lastSavedText = text
        lastHadBom = hadBom
        lastLineEnding = lineEnding
        _state.value = SaveState.Saved
    }
    
    /**
     * Marca el documento como sucio si estaba guardado.
     * Se llama cada vez que el usuario modifica el texto.
     */
    fun markDirty() {
        if (_state.value is SaveState.Saved) {
            _state.value = SaveState.Dirty
        }
    }
    
    /**
     * Guarda el texto actual en disco de forma atómica y serializada.
     * 
     * - Si el texto no ha cambiado respecto a [lastSavedText], no escribe a disco (idempotencia).
     * - Si ya hay un guardado en curso, espera a que termine.
     * - Preserva el BOM y el salto de línea original.
     */
    suspend fun save(currentText: String): Outcome<Unit> {
        val path = currentPath ?: return Outcome.Failure(AppError.NotFound)
        
        // Idempotencia: si el texto no cambió y ya estamos en estado Saved, no hacer nada.
        if (currentText == lastSavedText && _state.value is SaveState.Saved) {
            return Outcome.Success(Unit)
        }
        
        return mutex.withLock {
            _state.value = SaveState.Saving
            val content = NoteContent(currentText, lastHadBom, lastLineEnding)
            val result = withContext(dispatchers.io) {
                noteIO.write(path, content)
            }
            when (result) {
                is Outcome.Success -> {
                    lastSavedText = currentText
                    _state.value = SaveState.Saved
                    Outcome.Success(Unit)
                }
                is Outcome.Failure -> {
                    _state.value = SaveState.Failed(result.error)
                    result
                }
            }
        }
    }
    
    /**
     * Actualiza la ruta interna del documento si el archivo ha sido renombrado o movido
     * desde el explorador mientras estaba abierto.
     */
    fun remap(oldPath: Path, newPath: Path) {
        if (currentPath == oldPath) {
            currentPath = newPath
        }
    }
    
    /**
     * Cierra el documento y libera los recursos.
     */
    fun close() {
        currentPath = null
        lastSavedText = null
        _state.value = SaveState.Saved
    }
}