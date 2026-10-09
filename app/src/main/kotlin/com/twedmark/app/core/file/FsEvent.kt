package com.twedmark.app.core.file

import okio.Path

/**
 * Eventos emitidos por el repositorio cuando el sistema de archivos cambia.
 * 
 * Permiten que otras capas (como el Editor) reaccionen a cambios externos:
 * - Si el usuario borra el archivo que está editando, el editor puede cerrar la pestaña.
 * - Si el usuario renombra/mueve el archivo, el editor puede actualizar su ruta interna (remap).
 */
sealed interface FsEvent {
    /** Se ha creado un nuevo archivo o carpeta. */
    data class FileCreated(val path: Path) : FsEvent
    
    /** Se ha borrado un archivo o carpeta. */
    data class FileDeleted(val path: Path) : FsEvent
    
    /** Se ha renombrado un elemento en la misma carpeta. */
    data class FileRenamed(val oldPath: Path, val newPath: Path) : FsEvent
    
    /** Se ha movido un elemento a otra carpeta. */
    data class FileMoved(val oldPath: Path, val newPath: Path) : FsEvent
}