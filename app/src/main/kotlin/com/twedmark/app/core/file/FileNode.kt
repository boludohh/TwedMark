package com.twedmark.app.core.file

import okio.Path

/**
 * Tipo de nodo en el árbol de archivos del workspace.
 * - Folder: carpeta
 * - Note: archivo .md (las notas se crean siempre como .md)
 * - Image: archivo de imagen (png, jpg, jpeg, webp, bmp)
 * - Unsupported: cualquier otra extensión o archivo sin extensión
 */
enum class NodeKind {
    Folder,
    Note,
    Image,
    Unsupported
}

/**
 * Nodo del árbol de archivos del workspace.
 * La raíz se llama "workspace" y tiene depth 0.
 * Los hijos tienen depth = depth del padre + 1.
 * El árbol se aplana según las carpetas expandidas.
 */
data class FileNode(
    val path: Path,
    val name: String,
    val depth: Int,
    val kind: NodeKind,
    val isExpanded: Boolean,
    val isProtected: Boolean,
    val sizeBytes: Long,
    val childCount: Int
)