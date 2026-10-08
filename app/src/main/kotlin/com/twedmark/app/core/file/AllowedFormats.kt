package com.twedmark.app.core.file

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

    // kindOf(path, isDirectory) se añadirá en el Paso 0.7 junto con NodeKind.
}