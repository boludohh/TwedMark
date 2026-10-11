package com.twedmark.app.feature.editor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.ApplicationScope
import com.twedmark.app.core.common.Outcome
import com.twedmark.app.core.file.FsEvent
import com.twedmark.app.core.file.NoteIO
import com.twedmark.app.feature.editor.data.EditorPrefs
import com.twedmark.app.feature.editor.domain.DocumentSaver
import com.twedmark.app.feature.editor.domain.OpenDocument
import com.twedmark.app.feature.editor.domain.SaveState
import com.twedmark.app.feature.explorer.data.WorkspaceRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path

data class EditorUiState(
    val document: OpenDocument? = null,
    val saveState: SaveState = SaveState.Saved,
    val isLoading: Boolean = false,
    val wordWrap: Boolean = true,
    val lineNumbers: Boolean = true
)

sealed interface EditorEvent {
    data class OpenFile(val path: Path) : EditorEvent
    data object ContentChanged : EditorEvent
    data class SnapshotReady(val text: String) : EditorEvent
    data object SaveClicked : EditorEvent
    data class FlushRequested(val reason: com.twedmark.app.core.common.FlushReason) : EditorEvent
    data object CloseDocument : EditorEvent
    data object ToggleWordWrap : EditorEvent
    data object ToggleLineNumbers : EditorEvent
    data object RetryAfterFailure : EditorEvent
    data object DiscardAfterFailure : EditorEvent
}

sealed interface EditorEffect {
    data class Message(val error: AppError) : EditorEffect
    data class ConfirmSaveFailure(val pendingPath: Path?) : EditorEffect
}

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val repository: WorkspaceRepository,
    private val documentSaver: DocumentSaver,
    private val prefs: EditorPrefs,
    private val noteIO: NoteIO,
    private val dispatchers: AppDispatchers,
    private val applicationScope: ApplicationScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 64)
    private val _contentChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    
    private var controller: EditorController? = null

    init {
        viewModelScope.launch {
            prefs.wordWrapFlow.collect { wrap ->
                _uiState.update { it.copy(wordWrap = wrap) }
                controller?.setWordWrap(wrap)
            }
        }
        viewModelScope.launch {
            prefs.lineNumbersFlow.collect { ln ->
                _uiState.update { it.copy(lineNumbers = ln) }
                controller?.setLineNumbers(ln)
            }
        }

        viewModelScope.launch {
            _events.collect { event ->
                handleEvent(event)
            }
        }

        viewModelScope.launch {
            _contentChanges
                .debounce(1000)
                .collect {
                    val text = controller?.currentText() ?: return@collect
                    _events.emit(EditorEvent.SnapshotReady(text))
                }
        }

        viewModelScope.launch {
            repository.events.collect { fsEvent ->
                handleFsEvent(fsEvent)
            }
        }
        
        viewModelScope.launch {
            documentSaver.state.collect { state ->
                _uiState.update { it.copy(saveState = state) }
            }
        }
        
        viewModelScope.launch {
            applicationScope.processLifecycleEvents.collect { reason ->
                _events.emit(EditorEvent.FlushRequested(reason))
            }
        }
    }

    fun setController(controller: EditorController) {
        this.controller = controller
        controller.setWordWrap(_uiState.value.wordWrap)
        controller.setLineNumbers(_uiState.value.lineNumbers)
        controller.setOnContentChangedListener {
            _contentChanges.tryEmit(Unit)
            _events.tryEmit(EditorEvent.ContentChanged)
        }
    }

    fun sendEvent(event: EditorEvent) {
        _events.tryEmit(event)
    }

    private suspend fun handleEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.OpenFile -> openFile(event.path)
            is EditorEvent.ContentChanged -> {}
            is EditorEvent.SnapshotReady -> {
                documentSaver.save(event.text)
            }
            is EditorEvent.SaveClicked -> {
                val text = controller?.currentText() ?: return
                documentSaver.save(text)
            }
            is EditorEvent.FlushRequested -> {
                val text = controller?.currentText() ?: return
                documentSaver.save(text)
            }
            is EditorEvent.CloseDocument -> {
                val text = controller?.currentText() ?: return
                documentSaver.save(text)
                documentSaver.close()
                _uiState.update { it.copy(document = null, saveState = SaveState.Saved) }
            }
            is EditorEvent.ToggleWordWrap -> {
                val newVal = !_uiState.value.wordWrap
                prefs.setWordWrap(newVal)
            }
            is EditorEvent.ToggleLineNumbers -> {
                val newVal = !_uiState.value.lineNumbers
                prefs.setLineNumbers(newVal)
            }
            is EditorEvent.RetryAfterFailure -> {
                val text = controller?.currentText() ?: return
                documentSaver.save(text)
            }
            is EditorEvent.DiscardAfterFailure -> {
                _uiState.update { it.copy(saveState = SaveState.Saved) }
            }
        }
    }

    private suspend fun openFile(path: Path) {
        val currentDoc = _uiState.value.document
        
        // Guardar el documento actual si tiene cambios sin guardar
        if (currentDoc != null && _uiState.value.saveState is SaveState.Dirty) {
            val text = controller?.currentText()
            if (text != null) {
                val result = documentSaver.save(text)
                if (result is Outcome.Failure) {
                    _effects.send(EditorEffect.ConfirmSaveFailure(path))
                    return
                }
            }
        }

        _uiState.update { it.copy(isLoading = true) }
        
        val readResult = withContext(dispatchers.io) {
            noteIO.read(path)
        }
        
        when (readResult) {
            is Outcome.Success -> {
                val content = readResult.value
                val doc = OpenDocument(
                    path = path,
                    displayName = path.name,
                    hadBom = content.hadBom,
                    lineEnding = content.lineEnding
                )
                
                // Cerrar el documento anterior y abrir el nuevo
                documentSaver.close()
                documentSaver.open(path, content.text, content.hadBom, content.lineEnding)
                
                // Cargar el texto en el editor (si el controller está listo)
                controller?.loadText(content.text)
                
                _uiState.update { it.copy(document = doc, isLoading = false) }
            }
            is Outcome.Failure -> {
                _uiState.update { it.copy(isLoading = false) }
                _effects.send(EditorEffect.Message(readResult.error))
            }
        }
    }

    private fun handleFsEvent(event: FsEvent) {
        val currentPath = _uiState.value.document?.path ?: return
        when (event) {
            is FsEvent.FileRenamed -> {
                if (event.oldPath == currentPath) {
                    documentSaver.remap(event.oldPath, event.newPath)
                    _uiState.update { 
                        it.copy(document = it.document?.copy(path = event.newPath, displayName = event.newPath.name)) 
                    }
                }
            }
            is FsEvent.FileMoved -> {
                if (event.oldPath == currentPath) {
                    documentSaver.remap(event.oldPath, event.newPath)
                    _uiState.update { 
                        it.copy(document = it.document?.copy(path = event.newPath, displayName = event.newPath.name)) 
                    }
                }
            }
            is FsEvent.FileDeleted -> {
                if (event.path == currentPath) {
                    documentSaver.close()
                    _uiState.update { it.copy(document = null, saveState = SaveState.Saved) }
                    _effects.trySend(EditorEffect.Message(AppError.NotFound))
                }
            }
            else -> {}
        }
    }
}