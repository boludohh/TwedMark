package com.twedmark.app.feature.editor.presentation

import kotlinx.coroutines.flow.StateFlow

/**
 * Posición del cursor en el editor (base 1 para mostrar en UI).
 */
data class CursorPosition(val line: Int, val column: Int)

/**
 * Contrato de abstracción sobre sora-editor.
 * El ViewModel interactúa solo con esta interfaz, no con la vista de Android.
 */
interface EditorController {
    fun currentText(): String
    fun loadText(text: String)
    val cursor: StateFlow<CursorPosition>
    val canUndo: StateFlow<Boolean>
    val canRedo: StateFlow<Boolean>
    
    fun undo()
    fun redo()
    fun insertAtCursor(text: String)
    fun wrapSelection(prefix: String, suffix: String)
    fun setWordWrap(enabled: Boolean)
    fun setLineNumbers(enabled: Boolean)
    fun focusAndShowKeyboard()
    
    fun setOnContentChangedListener(listener: () -> Unit)
    fun release()
}