package com.twedmark.app.core.common

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
}