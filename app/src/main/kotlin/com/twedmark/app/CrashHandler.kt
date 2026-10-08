package com.twedmark.app

import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manejador global de excepciones no capturadas.
 * Escribe logs en filesDir/.twedmark/crash/ y conserva solo los 10 más recientes.
 * Se instala en Application.onCreate antes de Koin.
 */
class CrashHandler(
    private val crashDir: File,
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val MAX_FILES = 10
        private const val TAG = "CrashHandler"

        fun install(filesDir: File) {
            try {
                val crashDir = File(filesDir, ".twedmark/crash")
                if (!crashDir.exists()) {
                    crashDir.mkdirs()
                }
                val previous = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler(crashDir, previous))
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo instalar CrashHandler", e)
            }
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "crash_$timestamp.txt"
            val file = File(crashDir, fileName)

            val content = buildString {
                appendLine("Fecha: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("Versión app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Fabricante: ${Build.MANUFACTURER}")
                appendLine("Modelo: ${Build.MODEL}")
                appendLine("Hilo: ${t.name}")
                appendLine()
                appendLine("Stack trace:")
                appendLine(Log.getStackTraceString(e))
            }

            file.writeText(content, Charsets.UTF_8)
            cleanupOldFiles()
        } catch (writeError: Exception) {
            Log.e(TAG, "No se pudo escribir el log de crash", writeError)
        } finally {
            previous?.uncaughtException(t, e)
        }
    }

    private fun cleanupOldFiles() {
        try {
            val files = crashDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("crash_") && it.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() }
                ?: return

            if (files.size > MAX_FILES) {
                files.drop(MAX_FILES).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron limpiar archivos antiguos", e)
        }
    }
}