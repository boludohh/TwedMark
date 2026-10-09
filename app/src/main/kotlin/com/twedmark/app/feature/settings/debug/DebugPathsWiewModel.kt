package com.twedmark.app.feature.settings.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.common.AppError
import com.twedmark.app.core.common.Outcome
import com.twedmark.app.core.file.AtomicFileWriter
import com.twedmark.app.core.file.LineEnding
import com.twedmark.app.core.file.MarkdownPath
import com.twedmark.app.core.file.NameSanitizer
import com.twedmark.app.core.file.NoteContent
import com.twedmark.app.core.file.NoteIO
import com.twedmark.app.core.file.WorkspaceManager
import com.twedmark.app.feature.explorer.data.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class DebugPathsViewModel(
    private val appContext: Context,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val fs = FileSystem.SYSTEM

    sealed interface CaseState {
        data object Pending : CaseState
        data object Running : CaseState
        data object Passed : CaseState
        data class Failed(val message: String) : CaseState
    }

    data class DebugCase(
        val name: String,
        val run: suspend (Path) -> String?
    )

    private val cases: List<DebugCase> = buildCases()

    private val _states = MutableStateFlow<Map<String, CaseState>>(
        cases.associate { it.name to CaseState.Pending }
    )
    val states: StateFlow<Map<String, CaseState>> = _states.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    val passedCount: Int get() = _states.value.values.count { it is CaseState.Passed }
    val failedCount: Int get() = _states.value.values.count { it is CaseState.Failed }
    val totalCount: Int get() = cases.size

    fun runAll() {
        if (_isRunning.value) return
        viewModelScope.launch(dispatchers.io) {
            _isRunning.value = true
            val baseDir = appContext.cacheDir.absolutePath.toPath()
            val debugDir = baseDir / "debug-paths"
            val tempBase = debugDir / System.currentTimeMillis().toString()
            try {
                fs.createDirectories(tempBase)
                for (case in cases) {
                    _states.value = _states.value.toMutableMap().apply {
                        this[case.name] = CaseState.Running
                    }
                    val result = runCatching { case.run(tempBase) }
                        .getOrElse { "EXCEPCIÓN: ${it.message}" }
                    _states.value = _states.value.toMutableMap().apply {
                        this[case.name] = if (result == null) CaseState.Passed else CaseState.Failed(result)
                    }
                }
            } finally {
                runCatching { fs.deleteRecursively(tempBase) }
                _isRunning.value = false
            }
        }
    }

    private fun buildCases(): List<DebugCase> =
        nameSanitizerCases() +
        markdownPathCases() +
        workspaceManagerCases() +
        atomicWriterCases() +
        noteIOCases() +
        treeCases()

    private fun case(name: String, block: suspend (Path) -> String?): DebugCase {
        return DebugCase(name) { tempBase ->
            val safeName = name.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val caseBase = tempBase / safeName
            if (fs.exists(caseBase)) fs.deleteRecursively(caseBase)
            fs.createDirectories(caseBase)
            try {
                block(caseBase)
            } finally {
                runCatching { fs.deleteRecursively(caseBase) }
            }
        }
    }

    private fun nameSanitizerCases(): List<DebugCase> = listOf(
        case("NS-validate-Rename_2026") { _ ->
            val r = NameSanitizer.validate("Rename_2026")
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("NS-validate-borrador_1") { _ ->
            val r = NameSanitizer.validate("-borrador_1")
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("NS-validate-espacio") { _ ->
            val r = NameSanitizer.validate("Rename 2026")
            val ok = r is com.twedmark.app.core.file.NameError.DisallowedChar && r.ch == " "
            if (ok) null else "Esperado: DisallowedChar(\" \"), Obtenido: $r"
        },
        case("NS-validate-Cancion") { _ ->
            val r = NameSanitizer.validate("Canción")
            val ok = r is com.twedmark.app.core.file.NameError.DisallowedChar && r.ch == "ó"
            if (ok) null else "Esperado: DisallowedChar(\"ó\"), Obtenido: $r"
        },
        case("NS-validate-Ano") { _ ->
            val r = NameSanitizer.validate("Año")
            val ok = r is com.twedmark.app.core.file.NameError.DisallowedChar && r.ch == "ñ"
            if (ok) null else "Esperado: DisallowedChar(\"ñ\"), Obtenido: $r"
        },
        case("NS-validate-emoji") { _ ->
            val r = NameSanitizer.validate("nota😀")
            val ok = r is com.twedmark.app.core.file.NameError.DisallowedChar && r.ch == "😀"
            if (ok) null else "Esperado: DisallowedChar(\"😀\"), Obtenido: $r"
        },
        case("NS-validate-punto") { _ ->
            val r = NameSanitizer.validate("a.b")
            val ok = r is com.twedmark.app.core.file.NameError.DisallowedChar && r.ch == "."
            if (ok) null else "Esperado: DisallowedChar(\".\"), Obtenido: $r"
        },
        case("NS-validate-dos-puntos") { _ ->
            val r = NameSanitizer.validate("..")
            val ok = r is com.twedmark.app.core.file.NameError.DisallowedChar && r.ch == "."
            if (ok) null else "Esperado: DisallowedChar(\".\"), Obtenido: $r"
        },
        case("NS-validate-vacio") { _ ->
            val r = NameSanitizer.validate("")
            if (r is com.twedmark.app.core.file.NameError.Empty) null else "Esperado: Empty, Obtenido: $r"
        },
        case("NS-validate-200-bytes") { _ ->
            val r = NameSanitizer.validate("a".repeat(200))
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("NS-validate-201-bytes") { _ ->
            val r = NameSanitizer.validate("a".repeat(201))
            val ok = r is com.twedmark.app.core.file.NameError.TooLong && r.bytes == 201
            if (ok) null else "Esperado: TooLong(201), Obtenido: $r"
        },
        case("NS-comparisonKey") { _ ->
            val ok = NameSanitizer.comparisonKey("Notas") == NameSanitizer.comparisonKey("notas")
            if (ok) null else "Esperado: true, Obtenido: false"
        },
        case("NS-sanitizeBase-valido") { _ ->
            val r = NameSanitizer.sanitizeBase("Rename_2026", "nota")
            if (r == "Rename_2026") null else "Esperado: Rename_2026, Obtenido: $r"
        },
        case("NS-sanitizeBase-espacio") { _ ->
            val r = NameSanitizer.sanitizeBase("Mi nota", "nota")
            if (r == "Mi_nota") null else "Esperado: Mi_nota, Obtenido: $r"
        },
        case("NS-sanitizeBase-tildes") { _ ->
            val r = NameSanitizer.sanitizeBase("Canción de Año", "nota")
            if (r == "Cancion_de_Ano") null else "Esperado: Cancion_de_Ano, Obtenido: $r"
        },
        case("NS-sanitizeBase-Cafe") { _ ->
            val e = "Cafe\u0301"
            val r = NameSanitizer.sanitizeBase(e, "nota")
            if (r == "Cafe") null else "Esperado: Cafe, Obtenido: $r"
        },
        case("NS-sanitizeBase-emoji") { _ ->
            val r = NameSanitizer.sanitizeBase("Viaje😀2026", "nota")
            if (r == "Viaje_2026") null else "Esperado: Viaje_2026, Obtenido: $r"
        },
        case("NS-sanitizeBase-parentesis") { _ ->
            val r = NameSanitizer.sanitizeBase("fotos (viaje)", "carpeta")
            if (r == "fotos_viaje") null else "Esperado: fotos_viaje, Obtenido: $r"
        },
        case("NS-sanitizeBase-solo-barras") { _ ->
            val r = NameSanitizer.sanitizeBase("///", "carpeta")
            if (r == "carpeta") null else "Esperado: carpeta, Obtenido: $r"
        },
        case("NS-sanitizeBase-japones") { _ ->
            val r = NameSanitizer.sanitizeBase("日本語", "nota")
            if (r == "nota") null else "Esperado: nota, Obtenido: $r"
        },
        case("NS-sanitizeBase-muy-largo") { _ ->
            val r = NameSanitizer.sanitizeBase("a".repeat(201), "nota")
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("NS-sanitizeNoteFileName-markdown") { _ ->
            val r = NameSanitizer.sanitizeNoteFileName("escritura(/md).markdown")
            if (r == "escritura_md.md") null else "Esperado: escritura_md.md, Obtenido: $r"
        },
        case("NS-sanitizeNoteFileName-MARKDOWN") { _ ->
            val r = NameSanitizer.sanitizeNoteFileName("Diario.MARKDOWN")
            if (r == "Diario.md") null else "Esperado: Diario.md, Obtenido: $r"
        },
        case("NS-sanitizeNoteFileName-puntos") { _ ->
            val r = NameSanitizer.sanitizeNoteFileName("v1.2.notes.md")
            if (r == "v1_2_notes.md") null else "Esperado: v1_2_notes.md, Obtenido: $r"
        },
        case("NS-sanitizeNoteFileName-png") { _ ->
            val r = NameSanitizer.sanitizeNoteFileName("foto.png")
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("NS-sanitizeImageFileName") { _ ->
            val r = NameSanitizer.sanitizeImageFileName("Foto Viaje.PNG")
            if (r == "Foto_Viaje.png") null else "Esperado: Foto_Viaje.png, Obtenido: $r"
        }
    )

    private fun markdownPathCases(): List<DebugCase> = listOf(
        case("MP-relativize-misma-carpeta") { tempBase ->
            val root = tempBase / "workspace"
            val from = root / "Diario.md"
            val target = root / "viaje" / "notas.md"
            val r = MarkdownPath.relativize(from, target)
            if (r == "viaje/notas.md") null else "Esperado: viaje/notas.md, Obtenido: $r"
        },
        case("MP-relativize-con-subida") { tempBase ->
            val root = tempBase / "workspace"
            val from = root / "viaje" / "notas.md"
            val target = root / "assets" / "foto.png"
            val r = MarkdownPath.relativize(from, target)
            if (r == "../assets/foto.png") null else "Esperado: ../assets/foto.png, Obtenido: $r"
        },
        case("MP-relativize-assets") { tempBase ->
            val root = tempBase / "workspace"
            val from = root / "Diario.md"
            val target = root / "assets" / "Foto_1.png"
            val r = MarkdownPath.relativize(from, target)
            if (r == "assets/Foto_1.png") null else "Esperado: assets/Foto_1.png, Obtenido: $r"
        },
        case("MP-encode-espacios") { _ ->
            val r = MarkdownPath.encode("mis fotos/imagen (1).png")
            val expected = "mis%20fotos/imagen%20%281%29.png"
            if (r == expected) null else "Esperado: $expected, Obtenido: $r"
        },
        case("MP-decode-espacios") { _ ->
            val r = MarkdownPath.decode("mis%20fotos/imagen%20%281%29.png")
            val expected = "mis fotos/imagen (1).png"
            if (r == expected) null else "Esperado: $expected, Obtenido: $r"
        },
        case("MP-decode-angulos") { _ ->
            val r = MarkdownPath.decode("<mis fotos/a.png>")
            if (r == "mis fotos/a.png") null else "Esperado: mis fotos/a.png, Obtenido: $r"
        },
        case("MP-decode-ancla") { _ ->
            val r = MarkdownPath.decode("a.png#seccion")
            if (r == "a.png") null else "Esperado: a.png, Obtenido: $r"
        },
        case("MP-decode-encode-porcentaje") { _ ->
            val r = MarkdownPath.decode(MarkdownPath.encode("100%.png"))
            if (r == "100%.png") null else "Esperado: 100%.png, Obtenido: $r"
        },
        case("MP-encode-numeral") { _ ->
            val r = MarkdownPath.encode("a#b.png")
            if (r == "a%23b.png") null else "Esperado: a%23b.png, Obtenido: $r"
        },
        case("MP-decode-invalido") { _ ->
            val r = MarkdownPath.decode("zz%zz")
            if (r == "zz%zz") null else "Esperado: zz%zz, Obtenido: $r"
        },
        case("MP-resolve-http") { tempBase ->
            val root = tempBase / "workspace"
            val from = root / "Diario.md"
            val r = MarkdownPath.resolve(from, "https://x.com/a.png", root)
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("MP-resolve-absoluto") { tempBase ->
            val root = tempBase / "workspace"
            val from = root / "Diario.md"
            val r = MarkdownPath.resolve(from, "/etc/hosts", root)
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("MP-resolve-fuera") { tempBase ->
            val root = tempBase / "workspace"
            val from = root / "Diario.md"
            val r = MarkdownPath.resolve(from, "../../fuera.png", root)
            if (r == null) null else "Esperado: null, Obtenido: $r"
        },
        case("MP-resolve-dentro") { tempBase ->
            val root = tempBase / "workspace"
            val from = root / "Diario.md"
            val r = MarkdownPath.resolve(from, "assets/Foto_1.png", root)
            val expected = root / "assets" / "Foto_1.png"
            if (r == expected) null else "Esperado: $expected, Obtenido: $r"
        }
    )

    private fun workspaceManagerCases(): List<DebugCase> = listOf(
        case("WM-ensureStructure") { caseBase ->
            val wm = WorkspaceManager(fs, caseBase)
            if (fs.exists(wm.workspaceRoot)) fs.deleteRecursively(wm.workspaceRoot)
            if (fs.exists(wm.assetsDir)) fs.deleteRecursively(wm.assetsDir)
            wm.ensureStructure()
            if (!fs.exists(wm.workspaceRoot) || !fs.exists(wm.assetsDir)) {
                "workspace/ o assets/ no fueron recreados"
            } else null
        },
        case("WM-checkMutable-workspace") { caseBase ->
            val wm = WorkspaceManager(fs, caseBase)
            wm.ensureStructure()
            val r = wm.checkMutable(wm.workspaceRoot)
            if (r is Outcome.Failure && r.error is AppError.ProtectedPath) null
            else "Esperado: Failure(ProtectedPath), Obtenido: $r"
        },
        case("WM-checkMutable-assets") { caseBase ->
            val wm = WorkspaceManager(fs, caseBase)
            wm.ensureStructure()
            val r = wm.checkMutable(wm.assetsDir)
            if (r is Outcome.Failure && r.error is AppError.ProtectedPath) null
            else "Esperado: Failure(ProtectedPath), Obtenido: $r"
        },
        case("WM-checkMutable-archivo-en-assets") { caseBase ->
            val wm = WorkspaceManager(fs, caseBase)
            wm.ensureStructure()
            val foto = wm.assetsDir / "Foto_1.png"
            val r = wm.checkMutable(foto)
            if (r is Outcome.Success) null else "Esperado: Success, Obtenido: $r"
        },
        case("WM-requireInsideWorkspace-fuera") { caseBase ->
            val wm = WorkspaceManager(fs, caseBase)
            wm.ensureStructure()
            val fuera = wm.workspaceRoot / ".." / ".." / "x"
            val r = wm.requireInsideWorkspace(fuera)
            if (r is Outcome.Failure && r.error is AppError.OutsideWorkspace) null
            else "Esperado: Failure(OutsideWorkspace), Obtenido: $r"
        }
    )

    private fun atomicWriterCases(): List<DebugCase> = listOf(
        case("AW-escritura-exitosa") { caseBase ->
            val writer = AtomicFileWriter(fs)
            val target = caseBase / "test.md"
            val w1 = writer.write(target, "hola".toByteArray())
            if (w1 !is Outcome.Success) return@case "Escritura 1 falló: $w1"
            val contenido = fs.read(target) { readUtf8() }
            if (contenido != "hola") return@case "Contenido incorrecto: $contenido"
            val w2 = writer.write(target, "adios".toByteArray())
            if (w2 !is Outcome.Success) return@case "Escritura 2 falló: $w2"
            val contenido2 = fs.read(target) { readUtf8() }
            if (contenido2 != "adios") return@case "Contenido 2 incorrecto: $contenido2"
            val tmpExists = fs.exists(caseBase / "test.md.tmp")
            if (tmpExists) "Quedó un .tmp huérfano" else null
        },
        case("AW-escritura-fallida") { caseBase ->
            val writer = AtomicFileWriter(fs)
            val target = caseBase / "carpeta-no-vacia"
            fs.createDirectories(target)
            fs.write(target / "hijo.txt") { writeUtf8("x") }
            val r = writer.write(target, "contenido".toByteArray())
            if (r !is Outcome.Failure) return@case "Esperado: Failure, Obtenido: $r"
            val carpetaIntacta = fs.exists(target / "hijo.txt")
            if (!carpetaIntacta) return@case "La carpeta fue modificada"
            val tmpExists = fs.exists(caseBase / "carpeta-no-vacia.tmp")
            if (tmpExists) "Quedó un .tmp huérfano" else null
        }
    )

    private fun noteIOCases(): List<DebugCase> = listOf(
        case("NoteIO-BOM-CRLF-ida-vuelta") { caseBase ->
            val writer = AtomicFileWriter(fs)
            val io = NoteIO(fs, writer)
            val target = caseBase / "test.md"
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            val original = bom + "línea 1\r\nlínea 2\r\n".toByteArray()
            fs.write(target) { write(original) }
            val readResult = io.read(target)
            if (readResult !is Outcome.Success) return@case "Lectura falló: $readResult"
            val content = readResult.value
            if (!content.hadBom) return@case "hadBom debería ser true"
            if (content.lineEnding != LineEnding.CRLF) return@case "lineEnding debería ser CRLF"
            if (content.text != "línea 1\nlínea 2\n") return@case "Texto normalizado incorrecto: ${content.text}"
            val writeResult = io.write(target, content)
            if (writeResult !is Outcome.Success) return@case "Escritura falló: $writeResult"
            val reRead = fs.read(target) { readByteArray() }
            if (!reRead.contentEquals(original)) "El archivo no es idéntico byte a byte" else null
        },
        case("NoteIO-FileTooLarge") { caseBase ->
            val writer = AtomicFileWriter(fs)
            val io = NoteIO(fs, writer)
            val target = caseBase / "grande.md"
            val size = 5L * 1024 * 1024 + 1
            fs.write(target) { write(ByteArray(size.toInt())) }
            val r = io.read(target)
            if (r is Outcome.Failure && r.error is AppError.FileTooLarge) null
            else "Esperado: Failure(FileTooLarge), Obtenido: $r"
        },
        case("NoteIO-NotUtf8") { caseBase ->
            val writer = AtomicFileWriter(fs)
            val io = NoteIO(fs, writer)
            val target = caseBase / "invalido.md"
            val invalid = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00.toByte())
            fs.write(target) { write(invalid) }
            val r = io.read(target)
            if (r is Outcome.Failure && r.error is AppError.NotUtf8) null
            else "Esperado: Failure(NotUtf8), Obtenido: $r"
        }
    )

    private fun treeCases(): List<DebugCase> = listOf(
        case("Tree-orden-colapsado") { caseBase ->
            val wm = WorkspaceManager(fs, caseBase)
            wm.ensureStructure()
            fs.write(wm.workspaceRoot / "Diario.md") { writeUtf8("") }
            fs.createDirectories(wm.workspaceRoot / "viaje")
            fs.write(wm.workspaceRoot / "viaje" / "notas.md") { writeUtf8("") }
            fs.write(wm.workspaceRoot / "viaje" / "foto.png") { writeUtf8("") }
            val repo = WorkspaceRepository(fs, wm, dispatchers)
            repo.refreshAll()
            val tree = repo.tree.value
            val assetsNode: com.twedmark.app.core.file.FileNode? = tree.find { node -> node.path == wm.assetsDir }
            if (assetsNode?.isProtected != true) return@case "assets no tiene isProtected=true"
            val rootIndex = tree.indexOfFirst { node: com.twedmark.app.core.file.FileNode -> node.path == wm.workspaceRoot }
            val diarioIndex = tree.indexOfFirst { node: com.twedmark.app.core.file.FileNode -> node.name == "Diario.md" }
            val viajeIndex = tree.indexOfFirst { node: com.twedmark.app.core.file.FileNode -> node.name == "viaje" }
            val assetsIndex = tree.indexOfFirst { node: com.twedmark.app.core.file.FileNode -> node.name == "assets" }
            if (rootIndex < 0 || diarioIndex < 0 || viajeIndex < 0 || assetsIndex < 0) {
                return@case "No se encontraron todos los nodos"
            }
            val ordenOk = viajeIndex < diarioIndex && diarioIndex < assetsIndex
            if (!ordenOk) "Orden incorrecto: viaje=$viajeIndex, diario=$diarioIndex, assets=$assetsIndex"
            else null
        },
        case("Tree-orden-expandido") { caseBase ->
            val wm = WorkspaceManager(fs, caseBase)
            wm.ensureStructure()
            fs.write(wm.workspaceRoot / "Diario.md") { writeUtf8("") }
            fs.createDirectories(wm.workspaceRoot / "viaje")
            fs.write(wm.workspaceRoot / "viaje" / "notas.md") { writeUtf8("") }
            fs.write(wm.workspaceRoot / "viaje" / "foto.png") { writeUtf8("") }
            val repo = WorkspaceRepository(fs, wm, dispatchers)
            repo.refreshAll()
            repo.toggleExpanded(wm.workspaceRoot / "viaje")
            val tree = repo.tree.value
            val notasIndex = tree.indexOfFirst { node: com.twedmark.app.core.file.FileNode -> node.name == "notas.md" }
            val fotoIndex = tree.indexOfFirst { node: com.twedmark.app.core.file.FileNode -> node.name == "foto.png" }
            val viajeIndex = tree.indexOfFirst { node: com.twedmark.app.core.file.FileNode -> node.name == "viaje" }
            if (notasIndex < 0 || fotoIndex < 0) return@case "Hijos de viaje no aparecen"
            if (notasIndex <= viajeIndex || fotoIndex <= viajeIndex) {
                return@case "Hijos no están después de viaje"
            }
            null
        }
    )
}