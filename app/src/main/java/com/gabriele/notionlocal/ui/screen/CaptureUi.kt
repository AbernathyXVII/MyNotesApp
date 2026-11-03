package com.gabriele.notionlocal.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.data.record.Recording
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.delay
import kotlin.math.sqrt

private val RecordRed = Color(0xFFE5484D)

/**
 * **Il blocco audio mentre registra** (dal 03/11/2026): il pallino rosso, il
 * tempo, quanto è forte il suono, e per finire "Stop" (il file va nel
 * blocco) o la crocetta (la registrazione se ne va). Vedi `AudioRecorder`.
 */
@Composable
internal fun RecordingBar(
    recording: Recording,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var level by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(recording) {
        while (true) {
            now = System.currentTimeMillis()
            level = sqrt(AudioRecorder.level())
            delay(LEVEL_EVERY_MS)
        }
    }
    val elapsed = (now - recording.startedAt).coerceAtLeast(0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recording:" + recording.blockId)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // Il pallino si accende e si spegne ogni mezzo secondo.
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if ((elapsed / 500) % 2 == 0L) RecordRed else RecordRed.copy(alpha = 0.35f))
        )
        Spacer(Modifier.width(8.dp))
        Text(elapsedText(elapsed), style = MaterialTheme.typography.titleSmall, color = NotionWhite)
        Spacer(Modifier.width(8.dp))
        Text(CaptureStrings.recording, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NotionGray400.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0.02f, 1f))
                    .background(RecordRed)
            )
        }
        IconButton(onClick = onDiscard) {
            Icon(Icons.Filled.Close, contentDescription = CaptureStrings.discard, tint = NotionGray400)
        }
        IconButton(onClick = onStop) {
            Icon(Icons.Filled.Stop, contentDescription = CaptureStrings.stop, tint = RecordRed)
        }
    }
}

/** "0:42", "12:05", "1:02:33". */
internal fun elapsedText(ms: Long): String {
    val seconds = ms / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * Comincia a registrare nel blocco audio [block], chiedendo prima il
 * microfono se serve. Una registrazione già in corso altrove lo dice.
 */
@Composable
internal fun rememberRecordStarter(block: BlockEntity): () -> Unit {
    val context = LocalContext.current
    val current by rememberUpdatedState(block)
    val start = {
        when {
            AudioRecorder.current.value != null -> toast(context, CaptureStrings.alreadyRecording)
            !AudioRecorder.start(context, current.id, current.pageId) -> toast(context, CaptureStrings.cannotRecord)
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else toast(context, CaptureStrings.micDenied)
    }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            start()
        } else {
            ask.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/**
 * **"Scan document"** (dal 03/11/2026): apre lo scanner di Google (ML Kit,
 * dentro Google Play Services) — i bordi del foglio trovati da sé, il
 * ritaglio, i filtri, più pagine di fila — e le pagine diventano foto al
 * posto della riga [String] (l'id del blocco), o sotto di lei. Il loro testo
 * si legge poi per la ricerca (`ImageTexts`).
 */
@Composable
internal fun rememberDocumentScanner(viewModel: PageEditorViewModel): (String) -> Unit {
    val context = LocalContext.current
    val store = remember(context) { PageImageStore(context) }
    // L'id della riga sopravvive anche se l'app, mentre si scansiona, viene ricreata.
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val blockId = target
        target = null
        if (result.resultCode != Activity.RESULT_OK || blockId == null) return@rememberLauncherForActivityResult
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty().map { it.imageUri }
        if (pages.isEmpty()) return@rememberLauncherForActivityResult
        viewModel.insertScannedPages(blockId, pages, store) { toast(context, CaptureStrings.scanFailed) }
    }
    return { blockId ->
        val activity = context.findActivity()
        if (activity == null) {
            toast(context, CaptureStrings.scanUnavailable)
        } else {
            target = blockId
            GmsDocumentScanning.getClient(scannerOptions()).getStartScanIntent(activity)
                .addOnSuccessListener { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
                .addOnFailureListener {
                    target = null
                    toast(context, CaptureStrings.scanUnavailable)
                }
        }
    }
}

private fun scannerOptions(): GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
    .setGalleryImportAllowed(true)
    .setPageLimit(MAX_SCAN_PAGES)
    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .build()

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val LEVEL_EVERY_MS = 120L
private const val MAX_SCAN_PAGES = 50
