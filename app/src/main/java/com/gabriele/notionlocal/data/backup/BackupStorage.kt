package com.gabriele.notionlocal.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Un backup salvato: dove sta, come si chiama, quando è nato e quanto pesa. */
data class BackupFile(val uri: Uri, val name: String, val createdAt: Long, val size: Long)

/**
 * **Dove stanno i backup sul telefono**. Su Android 10 e dopo in
 * Download/notE backup (`MediaStoreStorage`): si vedono dall'app dei file e
 * restano anche disinstallando notE. Prima, nella cartella dell'app sulla
 * memoria del telefono. Nei test, una cartella qualsiasi (`DirectoryStorage`).
 */
interface BackupStorage {
    /** I backup, dal più nuovo. */
    fun list(): List<BackupFile>

    /** Un backup nuovo di nome [name]: [writer] ci scrive dentro. Se fallisce, non resta niente a metà. */
    fun write(name: String, writer: (OutputStream) -> Unit): BackupFile

    fun delete(file: BackupFile): Boolean

    fun open(file: BackupFile): InputStream
}

/** Il nome di un backup: "notE-backup-2026-10-28-1432.zip". */
internal fun isBackupName(name: String) = name.startsWith("notE-backup-") && name.endsWith(".zip")

/** Una cartella normale: per i telefoni prima di Android 10 e per i test. */
class DirectoryStorage(private val dir: File) : BackupStorage {

    override fun list(): List<BackupFile> =
        (dir.listFiles() ?: emptyArray()).filter { it.isFile && isBackupName(it.name) }
            .map { BackupFile(Uri.fromFile(it), it.name, it.lastModified(), it.length()) }
            .sortedWith(compareByDescending<BackupFile> { it.createdAt }.thenByDescending { it.name })

    override fun write(name: String, writer: (OutputStream) -> Unit): BackupFile {
        dir.mkdirs()
        val target = File(dir, name)
        val partial = File(dir, "$name.part")
        try {
            partial.outputStream().use(writer)
            if (!partial.renameTo(target)) throw IOException("Non si rinomina $partial")
        } finally {
            partial.delete()
        }
        return BackupFile(Uri.fromFile(target), name, target.lastModified(), target.length())
    }

    override fun delete(file: BackupFile): Boolean = File(dir, file.name).delete()

    override fun open(file: BackupFile): InputStream = File(dir, file.name).inputStream()
}

/**
 * Download/notE backup, da Android 10: i file li crea notE stessa, quindi
 * non serve nessun permesso per scriverli, leggerli e cancellarli. Mentre si
 * scrive il file è "in sospeso" e non si vede; finito, compare.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaStoreStorage(private val context: Context) : BackupStorage {

    private val collection: Uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + Backups.FOLDER + "/"

    override fun list(): List<BackupFile> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val found = mutableListOf<BackupFile>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.IS_PENDING} = 0",
            arrayOf(relativePath),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                if (!isBackupName(name)) continue
                found += BackupFile(
                    uri = ContentUris.withAppendedId(collection, cursor.getLong(0)),
                    name = name,
                    createdAt = cursor.getLong(3) * 1000,
                    size = cursor.getLong(2)
                )
            }
        }
        return found.sortedWith(compareByDescending<BackupFile> { it.createdAt }.thenByDescending { it.name })
    }

    override fun write(name: String, writer: (OutputStream) -> Unit): BackupFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Download non disponibile")
        try {
            (resolver.openOutputStream(uri, "w") ?: throw IOException("Non si apre $uri")).use(writer)
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return list().firstOrNull { it.uri == uri } ?: BackupFile(uri, name, System.currentTimeMillis(), 0)
    }

    override fun delete(file: BackupFile): Boolean =
        runCatching { context.contentResolver.delete(file.uri, null, null) > 0 }.getOrDefault(false)

    override fun open(file: BackupFile): InputStream =
        context.contentResolver.openInputStream(file.uri) ?: throw IOException("Non si apre ${file.uri}")
}
