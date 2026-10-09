package com.twedmark.app.feature.editor.domain

import com.twedmark.app.core.file.LineEnding
import okio.Path

/**
 * Representa el documento actualmente abierto en el editor.
 */
data class OpenDocument(
    val path: Path,
    val displayName: String,
    val hadBom: Boolean,
    val lineEnding: LineEnding
)