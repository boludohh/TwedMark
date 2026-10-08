package com.twedmark.app.core.file

import java.text.Normalizer
import java.util.Locale

/** Máximo de bytes UTF-8 permitidos para un nombre de archivo o carpeta. */
const val MAX_NAME_BYTES = 200

/** Caracteres permitidos: A-Z, a-z, 0-9, '_' y '-'. Cualquier otro es no permitido. */
val ALLOWED_NAME_REGEX = Regex("^[A-Za-z0-9_-]+$")

/** Errores que puede devolver la validación de nombres. */
sealed interface NameError {
    data object Empty : NameError
    data class DisallowedChar(val ch: String) : NameError
    data class TooLong(val bytes: Int) : NameError
    data object Duplicate : NameError
}

/**
 * Valida los nombres que escribe el usuario al crear o renombrar (sin extensión)
 * y sanea nombres al restaurar un ZIP. Lógica pura, sin dependencias de Android.
 */
object NameSanitizer {

    private val MARKS_REGEX = Regex("\\p{Mn}")

    /** null = válido. No recorta ni transforma el texto recibido. */
    fun validate(raw: String): NameError? {
        if (raw.isEmpty()) return NameError.Empty

        var index = 0
        while (index < raw.length) {
            val codePoint = raw.codePointAt(index)
            val charCount = Character.charCount(codePoint)
            if (!isAllowed(codePoint)) {
                return NameError.DisallowedChar(raw.substring(index, index + charCount))
            }
            index += charCount
        }

        val bytes = raw.toByteArray(Charsets.UTF_8).size
        if (bytes > MAX_NAME_BYTES) return NameError.TooLong(bytes)
        return null
    }

    /** Normaliza a minúsculas para detectar duplicados. */
    fun comparisonKey(name: String): String = name.lowercase(Locale.ROOT)

    /**
     * Sanea un nombre base al restaurar un ZIP.
     * Devuelve null si el resultado supera 200 bytes (quien llama omite el archivo).
     */
    fun sanitizeBase(raw: String, fallback: String): String? {
        if (ALLOWED_NAME_REGEX.matches(raw) &&
            raw.toByteArray(Charsets.UTF_8).size <= MAX_NAME_BYTES
        ) {
            return raw
        }

        val withoutMarks = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(MARKS_REGEX, "")

        val builder = StringBuilder()
        var pendingUnderscore = false
        var index = 0
        while (index < withoutMarks.length) {
            val codePoint = withoutMarks.codePointAt(index)
            val charCount = Character.charCount(codePoint)
            if (isAllowed(codePoint)) {
                if (pendingUnderscore) {
                    builder.append('_')
                    pendingUnderscore = false
                }
                builder.appendCodePoint(codePoint)
            } else {
                pendingUnderscore = true
            }
            index += charCount
        }

        var result = builder.toString().trim('_')
        if (result.isEmpty()) result = fallback
        if (result.toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES) return null
        return result
    }

    /** Sanea un nombre de archivo de nota (.md / .markdown) al restaurar un ZIP. */
    fun sanitizeNoteFileName(rawFileName: String): String? {
        val baseAndExt = splitBaseAndExtension(rawFileName) ?: return null
        val extension = baseAndExt.second.lowercase(Locale.ROOT)
        if (extension !in AllowedFormats.importNoteExtensions) return null
        val base = sanitizeBase(baseAndExt.first, fallback = "nota") ?: return null
        return "$base.md"
    }

    /** Sanea un nombre de archivo de imagen al restaurar un ZIP. */
    fun sanitizeImageFileName(rawFileName: String): String? {
        val baseAndExt = splitBaseAndExtension(rawFileName) ?: return null
        val extension = baseAndExt.second.lowercase(Locale.ROOT)
        if (extension !in AllowedFormats.imageExtensions) return null
        val base = sanitizeBase(baseAndExt.first, fallback = "imagen") ?: return null
        return "$base.$extension"
    }

    private fun splitBaseAndExtension(rawFileName: String): Pair<String, String>? {
        val lastDot = rawFileName.lastIndexOf('.')
        if (lastDot < 0) return null
        val base = rawFileName.substring(0, lastDot)
        val extension = rawFileName.substring(lastDot + 1)
        return base to extension
    }

    private fun isAllowed(codePoint: Int): Boolean =
        codePoint in 0x41..0x5A || // A-Z
        codePoint in 0x61..0x7A || // a-z
        codePoint in 0x30..0x39 || // 0-9
        codePoint == 0x5F ||       // _
        codePoint == 0x2D          // -
}