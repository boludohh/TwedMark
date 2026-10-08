package com.twedmark.app.core.file

import okio.Path

/**
 * Conjuntos de extensiones permitidas por tipo de archivo.
 * La extensión se compara siempre en minúsculas.
 */
object AllowedFormats {
    /** Las notas se crean siempre como .md */
    val noteExtensions = setOf("md")

    /** ".markdown" solo se acepta al importar o restaurar */
    val importNoteExtensions = setOf("md", "markdown")

    /** Extensiones de imagen aceptadas */
    val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "bmp")

    /**
     * Determina el tipo de nodo según la ruta y si es directorio.
     * - Si es directorio → Folder
     * - Si la extensión (minúsculas) está en noteExtensions → Note
     * - Si está en imageExtensions → Image
     * - Si no → Unsupported
     */
    fun kindOf(path: Path, isDirectory: Boolean): NodeKind {
        if (isDirectory) return NodeKind.Folder
        val extension = path.name.substringAfterLast('.', "").lowercase()
        return when (extension) {
            in noteExtensions -> NodeKind.Note
            in imageExtensions -> NodeKind.Image
            else -> NodeKind.Unsupported
        }
    }
}