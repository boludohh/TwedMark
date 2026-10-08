package com.twedmark.app.core.file

import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.Outcome
import java.io.File
import java.io.FileOutputStream
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Escritura atómica con forzado a disco antes del renombrado.
 *
 * Algoritmo: escribir en `.tmp` -> forzar a disco -> `atomicMove` al nombre final.
 * Si cualquier paso falla: se borra el `.tmp`, no se toca el original y se devuelve
 * `Failure(WriteFailed)`. Nunca se reporta éxito si no se llegó al renombrado.
 */
class AtomicFileWriter(private val fs: FileSystem) {

    fun write(target: Path, bytes: ByteArray): Outcome<Unit> {
        val parent = target.parent
            ?: return Outcome.Failure(AppError.WriteFailed(null))
        val tmp = parent / (target.name + ".tmp")
        return try {
            // Pasos 1-2: escribir todos los bytes en el temporal, cerrar y vaciar el buffer.
            fs.sink(tmp).buffer().use { it.write(bytes) }

            // Paso 3: forzar a disco. En Okio 3.x, FileHandle.flush() NO ejecuta fsync
            // en JVM/Android (JvmFileHandle.protectedFlush() esta vacio), asi que este es
            // el unico punto de excepcion documentado que garantiza la durabilidad
            // de los bytes antes del renombrado atomico.
            FileOutputStream(File(tmp.toString()), true).use { it.fd.sync() }

            // Paso 4: renombrado atomico. Solo a partir de aqui se reporta exito.
            fs.atomicMove(tmp, target)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            // Cualquier fallo: borrar el temporal y no tocar jamas el original.
            runCatching { if (fs.exists(tmp)) fs.delete(tmp) }
            Outcome.Failure(AppError.WriteFailed(e))
        }
    }
}