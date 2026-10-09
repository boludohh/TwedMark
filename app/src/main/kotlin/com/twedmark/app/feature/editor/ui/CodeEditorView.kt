package com.twedmark.app.feature.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.twedmark.app.feature.editor.presentation.CursorPosition
import com.twedmark.app.feature.editor.presentation.EditorController
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wrapper de Compose para sora-editor.
 * Crea la vista nativa y expone el controlador al ViewModel.
 */
@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    onControllerReady: (EditorController) -> Unit
) {
    val controllerImpl = remember { CodeEditorControllerImpl() }
    
    DisposableEffect(Unit) {
        onControllerReady(controllerImpl)
        onDispose {
            controllerImpl.release()
        }
    }

    AndroidView(
        factory = { context ->
            val editor = CodeEditor(context)
            controllerImpl.bind(editor)
            editor
        },
        modifier = modifier,
        onRelease = {
            // La liberación de recursos se maneja en el DisposableEffect
        }
    )
}

/**
 * Implementación real de EditorController sobre CodeEditor.
 */
class CodeEditorControllerImpl : EditorController {
    private var editor: CodeEditor? = null
    
    private val _cursor = MutableStateFlow(CursorPosition(1, 1))
    override val cursor: StateFlow<CursorPosition> = _cursor.asStateFlow()
    
    private val _canUndo = MutableStateFlow(false)
    override val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    private val _canRedo = MutableStateFlow(false)
    override val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    
    private var isLoadingText = false
    private var onContentChanged: (() -> Unit)? = null
    
    fun bind(editor: CodeEditor) {
        this.editor = editor
        
        // Listener de cambios de contenido (se pasa la clase explícitamente por ser método genérico de Java)
        editor.subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
            if (!isLoadingText) {
                onContentChanged?.invoke()
            }
            updateUndoRedo()
        }
        
        // Listener de cambios de selección/cursor
        editor.subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
            val left = event.left
            // sora-editor usa base 0, nosotros exponemos base 1 para la UI
            _cursor.value = CursorPosition(left.line + 1, left.column + 1)
        }
        
        // Configuración inicial por defecto (llamada explícita para evitar conflicto de sobrecargas en Kotlin)
        editor.setWordwrap(false)
        editor.setLineNumberEnabled(true)
    }
    
    override fun setOnContentChangedListener(listener: () -> Unit) {
        onContentChanged = listener
    }
    
    override fun currentText(): String {
        return editor?.text?.toString() ?: ""
    }
    
    override fun loadText(text: String) {
        isLoadingText = true
        editor?.setText(text)
        isLoadingText = false
        updateUndoRedo()
    }
    
    override fun undo() {
        editor?.undo()
        updateUndoRedo()
    }
    
    override fun redo() {
        editor?.redo()
        updateUndoRedo()
    }
    
    override fun insertAtCursor(text: String) {
        editor?.insertText(text, false)
        updateUndoRedo()
    }
    
    override fun wrapSelection(prefix: String, suffix: String) {
        val editor = this.editor ?: return
        val cursor = editor.cursor
        val left = cursor.left()
        val right = cursor.right()
        
        if (left.line == right.line && left.column == right.column) {
            // Sin selección: insertar prefijo + sufijo y mover cursor al medio
            editor.insertText(prefix + suffix, false)
            val newCol = left.column + prefix.length
            // En sora-editor, el método para mover el cursor simple es set(line, column)
            cursor.set(left.line, newCol)
        } else {
            // Con selección: envolver el texto seleccionado
            val selectedText = editor.text.substring(left.index, right.index)
            val newText = prefix + selectedText + suffix
            editor.text.replace(left.line, left.column, right.line, right.column, newText)
            cursor.set(right.line, right.column + prefix.length + suffix.length)
        }
        updateUndoRedo()
    }
    
    override fun setWordWrap(enabled: Boolean) {
        editor?.setWordwrap(enabled)
    }
    
    override fun setLineNumbers(enabled: Boolean) {
        editor?.setLineNumberEnabled(enabled)
    }
    
    override fun focusAndShowKeyboard() {
        editor?.requestFocus()
    }
    
    override fun release() {
        editor?.release()
        editor = null
    }
    
    private fun updateUndoRedo() {
        _canUndo.value = editor?.canUndo() ?: false
        _canRedo.value = editor?.canRedo() ?: false
    }
}