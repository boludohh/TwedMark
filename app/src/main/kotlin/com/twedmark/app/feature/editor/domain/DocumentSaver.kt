package com.twedmark.app.feature.editor.domain

import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.Outcome
import com.twedmark.app.core.file.LineEnding
import com.twedmark.app.core.file.NoteContent
import com.twedmark.app.core.file.NoteIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path

sealed interface SaveState {
    data object Saved : SaveState
    data object Dirty : SaveState
    data object Saving : SaveState
    data class Failed(val error: AppError) : SaveState
}

class DocumentSaver(
    private val noteIO: NoteIO,
    private val dispatchers: AppDispatchers
) {
    private val mutex = Mutex()
    
    private var currentPath: Path? = null
    private var lastSavedText: String? = null
    private var lastHadBom: Boolean = false
    private var lastLineEnding: LineEnding = LineEnding.LF
    
    private val _state = MutableStateFlow<SaveState>(SaveState.Saved)
    val state: StateFlow<SaveState> = _state.asStateFlow()
    
    fun open(path: Path, text: String, hadBom: Boolean, lineEnding: LineEnding) {
        currentPath = path
        lastSavedText = text
        lastHadBom = hadBom
        lastLineEnding = lineEnding
        _state.value = SaveState.Saved
    }
    
    fun markDirty() {
        if (_state.value is SaveState.Saved) {
            _state.value = SaveState.Dirty
        }
    }
    
    suspend fun save(currentText: String): Outcome<Unit> {
        val path = currentPath ?: return Outcome.Failure(AppError.NotFound)
        
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
    
    fun remap(oldPath: Path, newPath: Path) {
        if (currentPath == oldPath) {
            currentPath = newPath
        }
    }
    
    fun close() {
        currentPath = null
        lastSavedText = null
        _state.value = SaveState.Saved
    }
}