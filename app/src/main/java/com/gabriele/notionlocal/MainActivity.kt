package com.gabriele.notionlocal

import com.gabriele.notionlocal.data.widgets.WidgetStore
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.gabriele.notionlocal.data.ink.InkPrefs
import com.gabriele.notionlocal.data.security.AppLock
import android.os.SystemClock
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.notify.NoteAlarms
import com.gabriele.notionlocal.data.notify.NotificationPermission
import com.gabriele.notionlocal.data.notify.NotificationTarget
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    /** Il permesso delle notifiche, chiesto quando serve: vedi `NotificationPermission`. */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // **Un ripristino preparato** (dal 28/10/2026): i file del backup al
        // loro posto prima di tutto, prima che le impostazioni o il database
        // vengano letti. Vedi `Backups.applyPendingRestore`.
        Backups.applyPendingRestore(applicationContext)
        // Le impostazioni prima di tutto: tema, lingua e formati le
        // leggono già i primi pixel disegnati.
        AppSettings.init(applicationContext)
        // I backup: l'ultimo fatto, l'ultimo caricato, il promemoria.
        Backups.init(applicationContext)
        // Dopo le impostazioni: il pomodoro, suonando, guarda se le
        // notifiche sono messe a tacere.
        WidgetStore.init(applicationContext)
        // Gli strumenti del disegno, come li si è lasciati.
        InkPrefs.init(applicationContext)
        // La password delle pagine bloccate, e quali sono aperte adesso.
        AppLock.init(applicationContext)
        // I Rollup scrivono numeri e date come il resto dell'app (dal 21/10/2026).
        com.gabriele.notionlocal.data.entity.RollupFormatting.formatter =
            com.gabriele.notionlocal.ui.screen.AppRollupFormatter
        // Le notifiche: i canali, e la sveglia rimessa alla prossima.
        NoteAlarms.init(applicationContext)
        // I timer partiti, con la loro notifica nella tendina.
        WidgetStore.restoreTimerNotifications()
        NotificationPermission.requester = ::askNotificationPermission
        // Aperta toccando una notifica: la pagina da mostrare.
        NotificationTarget.takeFrom(intent)
        // Il tema "System" deve sapere com'è il telefono. Quando il
        // telefono cambia modalità l'attività viene ricreata e si passa di
        // nuovo di qui, quindi basta leggerlo ora.
        AppSettings.systemInDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        applySystemBars(AppSettings.isDark)

        // Un'unica ViewModelFactory per tutta l'app, passata giù ad ogni
        // schermata: costruisce Repository e AppDatabase una sola volta
        // (sono `by lazy` dentro la factory), quindi non c'è overhead a
        // ricrearla qui in cima.
        val viewModelFactory = ViewModelFactory(applicationContext)

        setContent {
            val dark = AppSettings.isDark
            // Le icone della barra del telefono (ora, batteria) devono
            // leggersi sullo sfondo della pagina: chiare sul tema scuro,
            // scure su quello chiaro.
            LaunchedEffect(dark) { applySystemBars(dark) }
            NotionLocalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(factory = viewModelFactory)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotificationTarget.takeFrom(intent)
    }

    override fun onStart() {
        super.onStart()
        // Rimasta fuori a lungo: le pagine bloccate si richiudono.
        AppLock.onAppStarted(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        super.onStop()
        AppLock.onAppStopped(SystemClock.elapsedRealtime())
        // Uscendo dall'app, la sveglia giusta: quello che è cambiato mentre
        // era aperta deve suonare anche ad app chiusa.
        NoteAlarms.reschedule()
        // E il backup di oggi, se manca (dal 28/10/2026).
        Backups.onAppStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (NotificationPermission.requester == ::askNotificationPermission) NotificationPermission.requester = null
    }

    /** Solo da Android 13, e solo se non c'è già: altrimenti non si chiede niente. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
