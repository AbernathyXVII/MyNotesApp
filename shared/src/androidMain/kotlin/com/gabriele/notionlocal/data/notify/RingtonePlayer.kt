package com.gabriele.notionlocal.data.notify

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * **Un suono del telefono, una volta sola** (fino al 04/11/2026 stava in
 * `WidgetStore`, che ora è nel codice comune): la fine di pomodori e timer,
 * e "Play sound" nelle impostazioni delle notifiche.
 */
object RingtonePlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Il suono che sta suonando adesso, per fermarlo: vedi `playOnce`. */
    private var playing: Ringtone? = null
    private var stopPlaying: Job? = null

    /**
     * Suona un suono **una volta sola, e al massimo per `MAX_SOUND_MS`**.
     *
     * Le suonerie del telefono (quelle "Galaxy" di Samsung, per esempio)
     * sono fatte per ripetersi finché non si risponde: il file stesso lo
     * chiede (l'etichetta `ANDROID_LOOP`), e suonate come un avviso non
     * finivano più — trovato sul telefono il 25/09/2026, scelta "Asteroid"
     * per il pomodoro. Si chiede di non ripetere (da Android 9), e in ogni
     * caso lo si ferma dopo qualche secondo: vale anche per un brano lungo
     * scelto come suono delle notifiche. Un suono nuovo ferma il vecchio.
     */
    fun playOnce(context: Context, uri: Uri) {
        stopPlaying?.cancel()
        playing?.stop()
        val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone.isLooping = false
        runCatching { ringtone.play() }
        playing = ringtone
        stopPlaying = scope.launch {
            delay(MAX_SOUND_MS)
            ringtone.stop()
            if (playing === ringtone) playing = null
        }
    }

    private const val MAX_SOUND_MS = 8_000L
}
