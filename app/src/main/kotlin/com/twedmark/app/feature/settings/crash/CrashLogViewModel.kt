package com.twedmark.app.feature.settings.crash

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CrashLog(
    val fileName: String,
    val date: String,
    val firstLine: String,
    val content: String
)

class CrashLogViewModel(application: Application) : AndroidViewModel(application) {

    private val crashDir: File = File(application.filesDir, ".twedmark/crash")

    private val _logs = MutableStateFlow<List<CrashLog>>(emptyList())
    val logs: StateFlow<List<CrashLog>> = _logs.asStateFlow()

    private val _selectedLog = MutableStateFlow<CrashLog?>(null)
    val selectedLog: StateFlow<CrashLog?> = _selectedLog.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshLogs()
    }

    fun refreshLogs() {
        viewModelScope.launch {
            _logs.value = withContext(Dispatchers.IO) {
                loadLogs()
            }
        }
    }

    fun selectLog(log: CrashLog) {
        _selectedLog.value = log
    }

    fun clearSelection() {
        _selectedLog.value = null
    }

    fun copyToClipboard() {
        val log = _selectedLog.value ?: return
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash log", log.content)
        clipboard.setPrimaryClip(clip)
        _message.value = context.getString(R.string.crash_log_copied)
    }

    fun share() {
        val log = _selectedLog.value ?: return
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Crash log: ${log.fileName}")
            putExtra(Intent.EXTRA_TEXT, log.content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.crash_log_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun deleteAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                crashDir.listFiles()?.forEach { it.delete() }
            }
            refreshLogs()
            _message.value = getApplication<Application>().getString(R.string.crash_log_all_deleted)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun loadLogs(): List<CrashLog> {
        val files = crashDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("crash_") && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: return emptyList()

        return files.map { file ->
            val content = file.readText(Charsets.UTF_8)
            val lines = content.lines()
            val date = lines.firstOrNull()?.removePrefix("Fecha: ")?.trim() ?: file.name
            val firstLine = lines.firstOrNull { it.isNotBlank() && !it.startsWith("Fecha:") }?.trim() ?: ""
            CrashLog(
                fileName = file.name,
                date = date,
                firstLine = firstLine,
                content = content
            )
        }
    }
}