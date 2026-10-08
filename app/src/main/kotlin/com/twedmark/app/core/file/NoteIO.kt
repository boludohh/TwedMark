package com.twedmark.app.core.file

import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.Outcome
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import okio.FileSystem
import okio.Path
import okio.buffer

enum class LineEnding { LF, CRLF }

data class NoteContent(val text: String, val hadBom: Boolean, val lineEnding: LineEnding)

/**
 * Lectura y escritura de notas en UTF-8 con manejo de BOM y de CRLF/LF.
 *
 * - `read`: rechaza archivos mayores de [MAX_OPEN_BYTES]; detecta y quita el BOM
 *   UTF-8 (lo recuerda en `hadBom`); detecta el salto predominante (CRLF si hay
 *   `\r\n`) y devuelve el texto siempre con `\n`; valida UTF-8 estricto.
 * - `write`: reconvierte a `lineEnding`, vuelve a poner el BOM si lo tenia y
 *   codifica siempre en UTF-8. Si el texto no es UTF-8 valido -> `NotUtf8`
 *   (nunca se guarda texto corrupto).
 */
class NoteIO(private val fs: FileSystem, private val writer: AtomicFileWriter) {

    companion object {
        const val MAX_OPEN_BYTES = 5L * 1024 * 1024
        private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }

    fun read(path: Path): Outcome<NoteContent> {
        return try {
            val size = fs.metadataOrNull(path)?.size ?: 0L
            if (size > MAX_OPEN_BYTES) return Outcome.Failure(AppError.FileTooLarge)

            val raw = fs.source(path).buffer().use { it.readByteArray() }

            var hadBom = false
            var offset = 0
            if (raw.size >= 3 && raw[0] == BOM[0] && raw[1] == BOM[1] && raw[2] == BOM[2]) {
                hadBom = true
                offset = 3
            }
            val body = if (offset == 0) raw else raw.copyOfRange(offset, raw.size)

            val text = decodeUtf8Strict(body)
                ?: return Outcome.Failure(AppError.NotUtf8)

            val lineEnding = if (text.contains("\r\n")) LineEnding.CRLF else LineEnding.LF
            val normalized = text.replace("\r\n", "\n")
            Outcome.Success(NoteContent(normalized, hadBom, lineEnding))
        } catch (e: Exception) {
            // Fallo de I/O durante la lectura (por ejemplo, archivo inexistente).
            Outcome.Failure(AppError.WriteFailed(e))
        }
    }

    fun write(path: Path, content: NoteContent): Outcome<Unit> {
        return try {
            val withEndings = when (content.lineEnding) {
                LineEnding.LF -> content.text
                LineEnding.CRLF -> content.text.replace("\n", "\r\n")
            }
            val body = encodeUtf8Strict(withEndings)
                ?: return Outcome.Failure(AppError.NotUtf8)
            val out = if (content.hadBom) BOM + body else body
            writer.write(path, out)
        } catch (e: Exception) {
            Outcome.Failure(AppError.WriteFailed(e))
        }
    }

    private fun decodeUtf8Strict(bytes: ByteArray): String? {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun encodeUtf8Strict(text: String): ByteArray? {
        val encoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            val buffer = encoder.encode(CharBuffer.wrap(text))
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        } catch (e: Exception) {
            null
        }
    }
}