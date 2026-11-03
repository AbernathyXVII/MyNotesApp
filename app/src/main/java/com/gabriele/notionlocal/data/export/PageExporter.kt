package com.gabriele.notionlocal.data.export

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Il file esportato, pronto da mandare, salvare o stampare. */
data class ExportedFile(val file: File, val mime: String) {
    val name: String get() = file.name
}

/** Cosa mettere nell'export, come scelto nella finestra "Export page". */
data class ExportOptions(
    val format: ExportFormat,
    val subpages: Boolean = false,
    /** I disegni a mano (dal 03/11/2026): solo nel PDF, che allora ha le proporzioni del telefono. */
    val drawings: Boolean = false,
    /** Per la stampa: un PDF solo, senza quelli delle pagine delle righe (che farebbero uno .zip). */
    val singleFile: Boolean = false
)

/**
 * **Prepara il file** di una pagina esportata, nella cartella "exports"
 * della cache dell'app (ogni volta svuotata: tiene solo l'ultimo).
 *
 * In Markdown una pagina con immagini, file, sottopagine o database diventa
 * un .zip, come fa Notion: "Pagina.md" e accanto la cartella "Pagina" con i
 * file, le sottopagine, il CSV di ogni database e le pagine delle sue righe,
 * a cui il testo porta con dei link. Senza, un .md da solo.
 *
 * In PDF le pagine delle righe dei database che hanno qualcosa di scritto
 * (dal 03/11/2026) diventano PDF a sé: allora il file è uno .zip con
 * "Pagina.pdf" e la cartella "Pagina/Database/Riga.pdf".
 */
class PageExporter(
    context: Context,
    private val repository: PageRepository,
    /** Chi legge i database come li mostra la loro vista; null = di un database resta il nome. */
    private val databases: DatabaseSource? = null
) {

    private val appContext = context.applicationContext
    private val store = PageImageStore(appContext)

    suspend fun export(pageId: String, options: ExportOptions, untitled: String): ExportedFile? =
        withContext(Dispatchers.IO) {
            val base = repository.exportPage(pageId, options.subpages) ?: return@withContext null
            // Un database a schermo intero: sotto il suo nome, la sua tabella.
            val shaped = if (base.page.isDatabase) {
                base.copy(blocks = listOf(BlockEntity(id = DATABASE_ROOT_BLOCK, pageId = base.page.id, type = BlockType.DATABASE_LINK, linkedPageId = base.page.id)))
            } else {
                base
            }
            val root = enrich(shaped, options, hashSetOf(pageId), 0, untitled)
            val directory = File(appContext.cacheDir, EXPORT_DIRECTORY).apply {
                deleteRecursively()
                mkdirs()
            }
            val name = exportFileName(root.page.title, untitled)
            when (options.format) {
                ExportFormat.PDF -> {
                    val pdf = PdfExport(::fileOf, untitled, if (options.drawings) PdfLayout.phone(appContext) else PdfLayout.DOCUMENT)
                    val pages = flatten(root)
                    if (options.singleFile || pages.none { page -> page.databases.values.any { db -> db.rows.any { it.page != null } } }) {
                        val file = File(directory, "$name.pdf")
                        FileOutputStream(file).use { out -> pdf.write(pages, out) }
                        ExportedFile(file, ExportFormat.PDF.mime)
                    } else {
                        val file = File(directory, "$name.zip")
                        ZipOutputStream(FileOutputStream(file)).use { zip -> writePdfs(zip, pages, folder = "", name = name, pdf = pdf, untitled = untitled) }
                        ExportedFile(file, ZIP_MIME)
                    }
                }
                ExportFormat.MARKDOWN -> {
                    if (root.subpages.isEmpty() && filesOf(root).isEmpty() && root.databases.isEmpty()) {
                        val file = File(directory, "$name.md")
                        file.writeText(markdownOf(root, untitled))
                        ExportedFile(file, ExportFormat.MARKDOWN.mime)
                    } else {
                        val file = File(directory, "$name.zip")
                        ZipOutputStream(FileOutputStream(file)).use { zip ->
                            writeMarkdown(zip, root, folder = "", name = name, untitled = untitled)
                        }
                        ExportedFile(file, ZIP_MIME)
                    }
                }
            }
        }

    /**
     * La pagina con quello che le serve per l'export oltre alle righe: i
     * bottoni a parole, i database come li mostra la loro vista (con le
     * pagine delle righe che hanno qualcosa, a loro volta complete) e, se
     * richiesti, i disegni. [seen] evita di esportare due volte la stessa
     * pagina in un giro di collegamenti.
     */
    private suspend fun enrich(page: ExportPage, options: ExportOptions, seen: MutableSet<String>, depth: Int, untitled: String): ExportPage {
        val strokes = if (options.drawings && options.format == ExportFormat.PDF) repository.strokesOf(page.page.id) else emptyList()
        val found = LinkedHashMap<String, ExportDatabase>()
        val reader = databases
        if (reader != null && depth < MAX_DEPTH) {
            for (block in page.blocks) {
                if (block.type != BlockType.DATABASE_LINK) continue
                val databaseId = block.linkedPageId ?: continue
                val read = runCatching { reader.read(databaseId) }.getOrNull() ?: continue
                val rows = read.database.rows.mapIndexed { index, row ->
                    val rowPageId = read.rowPageIds.getOrNull(index)
                    if (rowPageId == null || !seen.add(rowPageId)) {
                        row
                    } else {
                        val rowPage = repository.exportPage(rowPageId, options.subpages)?.let { enrich(it, options, seen, depth + 1, untitled) }
                        row.copy(page = rowPage?.takeIf(::hasContent))
                    }
                }
                found[block.id] = read.database.copy(rows = rows)
            }
        }
        return page.copy(
            strokes = strokes,
            buttons = buttonsOf(page, untitled),
            databases = found,
            subpages = page.subpages.map { enrich(it, options, seen, depth + 1, untitled) }
        )
    }

    /** I bottoni della pagina a parole; una copia collegata dice le azioni del bottone vero. */
    private suspend fun buttonsOf(page: ExportPage, untitled: String): Map<String, ExportButton> {
        val result = LinkedHashMap<String, ExportButton>()
        for (block in page.blocks) {
            if (block.type != BlockType.BUTTON) continue
            val attrs = block.attrs
            val config = attrs.button ?: attrs.buttonLink?.let { repository.blockOf(it)?.attrs?.button } ?: continue
            val (pageIds, databaseIds) = buttonReferences(config)
            val names = ButtonNames(repository.titlesOf(pageIds), databaseIds.associateWith { repository.columnsOf(it) })
            result[block.id] = exportButton(config, names, untitled)
        }
        return result
    }

    private fun fileOf(fileName: String): File? = store.fileFor(fileName).takeIf { it.isFile }

    /** I file dei blocchi della pagina che ci sono davvero: le immagini, i video, gli allegati. */
    private fun filesOf(page: ExportPage): List<String> =
        page.blocks.mapNotNull { it.attrs.file }.distinct().filter { fileOf(it) != null }

    /**
     * [pages] in "[folder][name].pdf"; le pagine delle righe dei loro database
     * in "[folder][name]/Database/Riga.pdf", e così via per i database di
     * quelle.
     */
    private fun writePdfs(zip: ZipOutputStream, pages: List<ExportPage>, folder: String, name: String, pdf: PdfExport, untitled: String) {
        zip.putNextEntry(ZipEntry("$folder$name.pdf"))
        pdf.write(pages, zip)
        zip.closeEntry()
        val used = HashSet<String>()
        for (page in pages) {
            for (database in page.databases.values) {
                val withPages = database.rows.filter { it.page != null }
                if (withPages.isEmpty()) continue
                val databaseName = unique(exportFileName(database.title, untitled), used)
                val rowNames = HashSet<String>()
                for (row in withPages) {
                    val rowName = unique(exportFileName(row.title, untitled) + ".pdf", rowNames).removeSuffix(".pdf")
                    writePdfs(zip, flatten(row.page!!), "$folder$name/$databaseName/", rowName, pdf, untitled)
                }
            }
        }
    }

    /**
     * La pagina [page] in "[folder][name].md", i suoi file, le sottopagine, il
     * CSV dei database e le pagine delle loro righe nella cartella
     * "[folder][name]/". I nomi uguali nella stessa cartella prendono un
     * numero ("foto 2.jpg").
     */
    private fun writeMarkdown(zip: ZipOutputStream, page: ExportPage, folder: String, name: String, untitled: String) {
        val inside = "$name/"
        val used = HashSet<String>()
        val fileLinks = filesOf(page).associateWith { fileName ->
            val attrsName = page.blocks.firstOrNull { it.attrs.file == fileName }?.attrs?.name
            var shown = exportFileName(attrsName ?: fileName, fileName)
            // Il nome com'era sul telefono a volte non ha l'estensione: quella del file salvato.
            if ('.' !in shown && '.' in fileName) shown += fileName.substring(fileName.lastIndexOf('.'))
            unique(shown, used)
        }
        val subpageNames = page.subpages.associate { sub ->
            sub.page.id to unique(exportFileName(sub.page.title, untitled) + ".md", used).removeSuffix(".md")
        }
        // Ogni database: "Nome.csv", e le pagine delle righe in "Nome/".
        val databaseNames = page.databases.mapValues { (_, database) ->
            unique(exportFileName(database.title, untitled) + ".csv", used).removeSuffix(".csv")
        }
        val rowNames = page.databases.mapValues { (_, database) ->
            val usedRows = HashSet<String>()
            database.rows.map { row -> row.page?.let { unique(exportFileName(row.title, untitled) + ".md", usedRows).removeSuffix(".md") } }
        }
        val text = markdownOf(
            page,
            untitled,
            fileLink = { fileLinks[it]?.let { stored -> inside + stored } },
            pageLink = { id -> subpageNames[id]?.let { sub -> "$inside$sub.md" } },
            databaseLinks = { blockId ->
                databaseNames[blockId]?.let { databaseName ->
                    MarkdownDatabaseLinks(
                        csv = "$inside$databaseName.csv",
                        rows = rowNames[blockId].orEmpty().map { row -> row?.let { "$inside$databaseName/$it.md" } }
                    )
                }
            }
        )
        zip.putNextEntry(ZipEntry("$folder$name.md"))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        for ((fileName, stored) in fileLinks) {
            val source = fileOf(fileName) ?: continue
            zip.putNextEntry(ZipEntry("$folder$inside$stored"))
            FileInputStream(source).use { it.copyTo(zip) }
            zip.closeEntry()
        }
        for ((blockId, database) in page.databases) {
            val databaseName = databaseNames.getValue(blockId)
            zip.putNextEntry(ZipEntry("$folder$inside$databaseName.csv"))
            zip.write(csvOf(database).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            database.rows.forEachIndexed { index, row ->
                val rowName = rowNames.getValue(blockId)[index] ?: return@forEachIndexed
                writeMarkdown(zip, row.page!!, "$folder$inside$databaseName/", rowName, untitled)
            }
        }
        for (sub in page.subpages) {
            writeMarkdown(zip, sub, folder + inside, subpageNames.getValue(sub.page.id), untitled)
        }
    }

    companion object {
        const val EXPORT_DIRECTORY = "exports"
        const val ZIP_MIME = "application/zip"

        /** Il blocco finto con cui un database a schermo intero si esporta come la sua tabella. */
        const val DATABASE_ROOT_BLOCK = "__database__"

        /** Quanti livelli di database dentro le pagine delle righe si seguono. */
        private const val MAX_DEPTH = 6

        /** La pagina e poi le sottopagine, in ordine di lettura. */
        fun flatten(page: ExportPage): List<ExportPage> = listOf(page) + page.subpages.flatMap(::flatten)

        /** [name] se nessun altro file nella stessa cartella si chiama così, altrimenti "nome 2.ext", "nome 3.ext"… */
        fun unique(name: String, used: MutableSet<String>): String {
            val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
            var candidate = name
            var n = 2
            while (!used.add(candidate.lowercase())) {
                candidate = name.substring(0, dot) + " " + n++ + name.substring(dot)
            }
            return candidate
        }

        private fun uriOf(context: Context, exported: ExportedFile): Uri =
            FileProvider.getUriForFile(context, "${context.packageName}.files", exported.file)

        /** **Manda** il file con un'altra app (WhatsApp, la posta, Drive…), con la scelta di sistema. */
        fun share(context: Context, exported: ExportedFile, title: String) {
            val chooser = shareIntent(uriOf(context, exported), exported, title)
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }

        /** La scelta di sistema delle app a cui mandare il file [uri]. */
        fun shareIntent(uri: Uri, exported: ExportedFile, title: String): Intent {
            val send = Intent(Intent.ACTION_SEND)
                .setType(exported.mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData = ClipData.newRawUri(exported.name, uri)
            return Intent.createChooser(send, null)
        }

        /** **Salva** il file dove l'utente ha scelto (vedi [SaveExport]). */
        suspend fun copyTo(context: Context, exported: ExportedFile, uri: Uri): Boolean = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                    FileInputStream(exported.file).use { it.copyTo(out) }
                }
            }.isSuccess
        }

        /** **Stampa** il PDF, con la finestra di stampa di sistema (che sa anche salvarlo come PDF). */
        fun print(context: Context, exported: ExportedFile, title: String) {
            val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val attributes = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
            manager.print(title, FilePrintAdapter(exported.file, exported.name), attributes)
        }
    }
}

/**
 * La finestra di sistema "Salva in…": il nome e il tipo del file sono quelli
 * del file esportato. Restituisce dove salvarlo, o null se l'utente ha
 * annullato.
 */
class SaveExport : ActivityResultContract<ExportedFile, Uri?>() {
    override fun createIntent(context: Context, input: ExportedFile): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mime)
            .putExtra(Intent.EXTRA_TITLE, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

/** Passa alla stampa un PDF già pronto: i fogli sono già impaginati in A4. */
private class FilePrintAdapter(private val file: File, private val name: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, oldAttributes == null)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { input.copyTo(it) }
            }
            if (cancellationSignal.isCanceled) callback.onWriteCancelled() else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}
