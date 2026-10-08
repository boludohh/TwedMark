package com.twedmark.app.core.file

import okio.Path

/**
 * Codifica, decodifica, relativiza y resuelve rutas de enlaces Markdown.
 *
 * Los nombres que crea la app son limpios, así que sus rutas casi nunca necesitan
 * codificación. Se mantiene este módulo porque (a) el usuario puede escribir a mano
 * enlaces con espacios o paréntesis, y (b) las notas importadas traen rutas con
 * `%20`, espacios y paréntesis que hay que interpretar para reescribirlas.
 *
 * Lógica pura: usa [okio.Path] y no depende de Android.
 */
object MarkdownPath {

    private val ESCAPES = mapOf(
        ' ' to "%20",
        '(' to "%28",
        ')' to "%29",
        '[' to "%5B",
        ']' to "%5D",
        '<' to "%3C",
        '>' to "%3E",
        '#' to "%23",
        '?' to "%3F"
    )

    /**
     * Codifica una ruta relativa segmento a segmento. El separador `/` no se codifica.
     * Tildes, ñ y emojis se dejan tal cual.
     */
    fun encode(relativePath: String): String =
        relativePath.split('/').joinToString(separator = "/") { encodeSegment(it) }

    /**
     * Decodifica un enlace: quita `<` y `>` si lo envuelven, quita un `#ancla` final
     * y decodifica `%XX` como bytes UTF-8. Una secuencia inválida (`%zz`) se deja
     * tal cual, sin lanzar excepción.
     */
    fun decode(link: String): String {
        var work = link
        if (work.length >= 2 && work.startsWith("<") && work.endsWith(">")) {
            work = work.substring(1, work.length - 1)
        }
        val hashIndex = work.indexOf('#')
        if (hashIndex >= 0) {
            work = work.substring(0, hashIndex)
        }
        return decodePercent(work)
    }

    /**
     * Calcula la ruta desde la carpeta de la nota hasta [target], usando `..` cuando
     * hace falta. Misma carpeta: sin prefijo `./`. Devuelve la ruta ya codificada.
     */
    fun relativize(fromNote: Path, target: Path): String {
        val fromDir = fromNote.parent ?: return encode(target.name)
        val fromSegments = fromDir.segments
        val targetSegments = target.segments

        var common = 0
        val max = minOf(fromSegments.size, targetSegments.size)
        while (common < max && fromSegments[common] == targetSegments[common]) {
            common++
        }

        val builder = StringBuilder()
        repeat(fromSegments.size - common) {
            if (builder.isNotEmpty()) builder.append('/')
            builder.append("..")
        }
        for (i in common until targetSegments.size) {
            if (builder.isNotEmpty()) builder.append('/')
            builder.append(targetSegments[i])
        }
        return encode(builder.toString())
    }

    /**
     * Resuelve un enlace relativo contra la carpeta de la nota y comprueba que quede
     * dentro de [workspaceRoot]. Devuelve `null` si no es una ruta local válida.
     */
    fun resolve(fromNote: Path, link: String, workspaceRoot: Path): Path? {
        if (link.startsWith("http://") ||
            link.startsWith("https://") ||
            link.startsWith("mailto:") ||
            link.startsWith("data:") ||
            link.startsWith("file:") ||
            link.startsWith("/")
        ) {
            return null
        }
        val fromDir = fromNote.parent ?: return null
        val normalized = (fromDir / decode(link)).normalized()
        val root = workspaceRoot.normalized()
        return if (isInside(normalized, root)) normalized else null
    }

    private fun encodeSegment(segment: String): String {
        // El '%' se sustituye primero para no codificar dos veces los escapes siguientes.
        val escapedPercent = segment.replace("%", "%25")
        val builder = StringBuilder(escapedPercent.length)
        for (ch in escapedPercent) {
            builder.append(ESCAPES[ch] ?: ch.toString())
        }
        return builder.toString()
    }

    private fun decodePercent(input: String): String {
        val builder = StringBuilder()
        val pendingBytes = ArrayList<Byte>()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch == '%' && i + 2 < input.length) {
                val value = input.substring(i + 1, i + 3).toIntOrNull(16)
                if (value != null) {
                    pendingBytes.add(value.toByte())
                    i += 3
                    continue
                }
            }
            if (pendingBytes.isNotEmpty()) {
                builder.append(String(pendingBytes.toByteArray(), Charsets.UTF_8))
                pendingBytes.clear()
            }
            builder.append(ch)
            i++
        }
        if (pendingBytes.isNotEmpty()) {
            builder.append(String(pendingBytes.toByteArray(), Charsets.UTF_8))
        }
        return builder.toString()
    }

    private fun isInside(path: Path, root: Path): Boolean {
        val pathSegments = path.segments
        val rootSegments = root.segments
        if (pathSegments.size < rootSegments.size) return false
        for (i in rootSegments.indices) {
            if (pathSegments[i] != rootSegments[i]) return false
        }
        return true
    }
}