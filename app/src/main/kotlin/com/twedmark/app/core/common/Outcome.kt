package com.twedmark.app.core.common

import com.twedmark.app.core.file.NameError

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

sealed interface AppError {
    data object ProtectedPath : AppError
    data object OutsideWorkspace : AppError
    data class WriteFailed(val cause: Throwable?) : AppError
    data object FileTooLarge : AppError
    data object NotUtf8 : AppError

    // --- Errores añadidos en Fase 1 (Paso 1.1) ---
    
    /** Envuelve un error de validación de nombre (vacío, caracteres inválidos, demasiado largo). */
    data class InvalidName(val error: NameError) : AppError
    
    /** Se intenta crear un archivo o carpeta con un nombre que ya existe en ese directorio. */
    data object AlreadyExists : AppError
    
    /** La ruta de origen no existe (p.ej. se borró externamente antes de completar la operación). */
    data object NotFound : AppError
    
    /** Se intenta mover una carpeta dentro de sí misma o de uno de sus descendientes. */
    data object MoveIntoItself : AppError
    
    /** La operación de movimiento haría que la profundidad del árbol supere el límite (8 niveles). */
    data object DepthLimitExceeded : AppError
}