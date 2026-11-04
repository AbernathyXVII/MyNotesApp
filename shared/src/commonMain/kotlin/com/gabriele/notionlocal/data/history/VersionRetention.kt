package com.gabriele.notionlocal.data.history

import com.gabriele.notionlocal.data.entity.VersionStamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * **Quante versioni di una pagina si tengono** (dal 02/11/2026): tutte
 * quelle delle ultime 24 ore; più indietro, una al giorno (l'ultima di quel
 * giorno) per 30 giorni; più vecchie, nessuna. L'ultima versione di una
 * pagina resta sempre, anche se ha mesi: è com'è la pagina adesso.
 */
object VersionRetention {

    const val KEEP_ALL_MILLIS = 24L * 60 * 60 * 1000
    const val KEEP_DAYS = 30L
    private const val KEEP_DAYS_MILLIS = KEEP_DAYS * KEEP_ALL_MILLIS

    /** Gli id delle versioni da togliere, fra tutte quelle di una pagina. */
    fun toDrop(versions: List<VersionStamp>, now: Long, zone: ZoneId): List<String> {
        val keptDays = HashSet<LocalDate>()
        val drop = ArrayList<String>()
        versions.sortedByDescending { it.at }.forEachIndexed { i, version ->
            val age = now - version.at
            val day = Instant.ofEpochMilli(version.at).atZone(zone).toLocalDate()
            when {
                age < KEEP_ALL_MILLIS -> Unit
                i == 0 -> keptDays += day
                age > KEEP_DAYS_MILLIS -> drop += version.id
                !keptDays.add(day) -> drop += version.id
            }
        }
        return drop
    }
}
