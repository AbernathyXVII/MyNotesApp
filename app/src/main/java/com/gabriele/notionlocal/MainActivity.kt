package com.gabriele.notionlocal

import com.gabriele.notionlocal.data.widgets.WidgetStore
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Le impostazioni prima di tutto: tema, lingua e formati le
        // leggono già i primi pixel disegnati.
        AppSettings.init(applicationContext)
        // Dopo le impostazioni: il pomodoro, suonando, guarda se le
        // notifiche sono messe a tacere.
        WidgetStore.init(applicationContext)
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

    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
