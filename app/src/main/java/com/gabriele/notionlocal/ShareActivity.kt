package com.gabriele.notionlocal

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.gabriele.notionlocal.data.notify.Notifier
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.share.Incoming
import com.gabriele.notionlocal.ui.i18n.ShareStrings
import com.gabriele.notionlocal.ui.screen.ShareSheet
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * **"Condividi con notE"** (dal 02/11/2026, chiesto dall'utente): la voce
 * "notE" nel menu Condividi di Chrome, della Galleria, di WhatsApp e delle
 * altre app. È trasparente: sopra l'app da cui si condivide sale solo la
 * finestra `ShareSheet`, e salvato si torna lì. Con "Save and open" si apre
 * notE sulla pagina, passando da `MainActivity` come per una notifica.
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema, lingua e formati, come all'avvio di `MainActivity`; e, come lì,
        // se il telefono è scuro: senza, col tema "System" le scritte venivano
        // nere sul fondo scuro della finestra.
        AppSettings.init(applicationContext)
        AppSettings.systemInDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val incoming = Incoming.from(intent)
        if (incoming == null || incoming.isEmpty) {
            Toast.makeText(this, ShareStrings.nothingToSave, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val factory = ViewModelFactory(applicationContext)
        setContent {
            NotionLocalTheme {
                // Il colore del testo che in `MainActivity` dà la `Surface` intorno a
                // tutto: senza, le scritte della finestra venivano nere sul fondo scuro.
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                ShareSheet(
                    incoming = incoming,
                    factory = factory,
                    onClose = { finish() },
                    onOpen = { pageId ->
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra(Notifier.EXTRA_PAGE_ID, pageId)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        finish()
                    }
                )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        // Niente animazione: la finestra è già scesa.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
