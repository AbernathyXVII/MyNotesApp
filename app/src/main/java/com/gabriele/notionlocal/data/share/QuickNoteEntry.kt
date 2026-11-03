package com.gabriele.notionlocal.data.share

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import com.gabriele.notionlocal.MainActivity
import com.gabriele.notionlocal.R

// ---------------------------------------------------------------------------
// **Le porte della nota al volo** (dal 02/11/2026, vedi `QuickNotes`): il
// tasto nelle impostazioni rapide (la tendina in alto) e il widget della
// schermata Home. Tutti e due aprono notE con `QuickNotes.ACTION`, come la
// scorciatoia che si vede tenendo premuta l'icona (`res/xml/shortcuts.xml`).
// ---------------------------------------------------------------------------

/** notE, aperta per una nota al volo. */
internal fun quickNoteIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(QuickNotes.ACTION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

internal fun quickNotePendingIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        QUICK_NOTE_REQUEST,
        quickNoteIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

private const val QUICK_NOTE_REQUEST = 4207

/**
 * **"Quick note" nelle impostazioni rapide.** Si aggiunge dalla matita della
 * tendina; toccandolo la tendina si chiude e si scrive. A telefono bloccato
 * chiede prima di sbloccarlo.
 */
class QuickNoteTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { open() } else open()
    }

    private fun open() {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(quickNotePendingIntent(this))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(quickNoteIntent(this))
        }
    }
}

/** **Il widget "Quick note"** della schermata Home: un tasto solo, che apre una nota al volo. */
class QuickNoteWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_note).apply {
            setOnClickPendingIntent(R.id.quick_note_widget, quickNotePendingIntent(context))
        }
        manager.updateAppWidget(appWidgetIds, views)
    }
}
