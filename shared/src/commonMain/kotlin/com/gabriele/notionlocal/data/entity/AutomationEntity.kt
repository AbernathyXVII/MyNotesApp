package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gabriele.notionlocal.data.buttons.ButtonStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

// ---------------------------------------------------------------------------
// **Le automazioni dei database** (dal 03/11/2026, chiesto dall'utente:
// "Automazioni a tempo, il passo dopo i bottoni: ogni lunedì crea la pagina
// della settimana, quando spunto Fatto, metti la data di oggi. Notion ha i
// modelli ricorrenti e le automazioni dei database"). Un'automazione è un
// **quando** ([AutomationTrigger]: una pagina aggiunta, una proprietà
// cambiata, un giorno e un'ora che tornano) e le **azioni dei bottoni**
// (`ButtonStep`), fatte dallo stesso motore senza finestre (vedi
// `data/automations/Automations.kt`).
// ---------------------------------------------------------------------------

/** Un'automazione di un database, salvata. */
@Entity(
    tableName = "automations",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["databaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("databaseId")]
)
data class AutomationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** Il database (la pagina col database, non una sua vista). */
    val databaseId: String,
    val name: String = "",
    /** Il quando e le azioni, vedi [AutomationConfig]. */
    val configJson: String = AutomationConfig.encode(AutomationConfig()),
    val enabled: Boolean = true,
    /** L'ultima volta che è partita; per quelle a orario, fin dove sono già state fatte. */
    val lastRunAt: Long? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val config: AutomationConfig get() = AutomationConfig.parse(configJson)
}

/** Il quando e le azioni di un'automazione. */
@Serializable
data class AutomationConfig(
    val trigger: AutomationTrigger = AutomationTrigger.PropertyEdited(),
    val actions: List<ButtonStep> = emptyList()
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun parse(text: String?): AutomationConfig =
            text?.let { runCatching { json.decodeFromString<AutomationConfig>(it) }.getOrNull() } ?: AutomationConfig()

        fun encode(config: AutomationConfig): String = json.encodeToString(serializer(), config)
    }
}

/** Ogni quanto torna un'automazione a orario. */
@Serializable
enum class Repeat { DAILY, WEEKLY, MONTHLY }

/** **Quando parte** un'automazione. */
@Serializable
sealed class AutomationTrigger {

    /** Una riga nuova nel database, comunque nata (a mano, da un bottone, condividendo…). */
    @Serializable
    @SerialName("page_added")
    data class PageAdded(val unused: Boolean = false) : AutomationTrigger()

    /**
     * Una proprietà cambiata ([columnId]). Con [values] solo quando diventa
     * uno di quei valori ("true" per una casella spuntata, i nomi delle
     * scelte di una Select); vuoto, a ogni cambiamento.
     */
    @Serializable
    @SerialName("property_edited")
    data class PropertyEdited(val columnId: String? = null, val values: List<String> = emptyList()) : AutomationTrigger() {

        /**
         * Se il passaggio da [before] ad [after] la fa partire. Con dei valori
         * scelti, solo quando ci arriva: già spuntata e rispuntata, no. Le
         * scelte multiple si guardano una per una ("Casa|Urgente").
         */
        fun fires(before: String, after: String): Boolean {
            if (before == after) return false
            if (values.isEmpty()) return true
            return matches(after) && !matches(before)
        }

        /** Se [value] è uno dei valori scelti (senza valori scelti, sempre). */
        fun matches(value: String): Boolean =
            values.isEmpty() || value.split(MULTI_VALUE_SEPARATOR).any { it in values }
    }

    /**
     * **A orario**: ogni giorno, ogni settimana nei giorni [days] (1 = lunedì,
     * 7 = domenica) o ogni mese il giorno [dayOfMonth] (l'ultimo, nei mesi
     * più corti), all'ora [minute] (minuti dalla mezzanotte).
     */
    @Serializable
    @SerialName("schedule")
    data class Schedule(
        val repeat: Repeat = Repeat.WEEKLY,
        val days: List<Int> = listOf(1),
        val dayOfMonth: Int = 1,
        val minute: Int = 9 * 60
    ) : AutomationTrigger() {

        fun fallsOn(date: LocalDate): Boolean = when (repeat) {
            Repeat.DAILY -> true
            Repeat.WEEKLY -> date.dayOfWeek.value in days
            Repeat.MONTHLY -> date.dayOfMonth == dayOfMonth.coerceIn(1, date.lengthOfMonth())
        }

        private fun at(date: LocalDate, zone: java.time.ZoneId): ZonedDateTime =
            date.atTime(LocalTime.of(minute / 60, minute % 60)).atZone(zone)

        /** L'ultima volta che toccava, fino a [now] compreso; null se non tocca mai (una settimana senza giorni). */
        fun lastOccurrence(now: ZonedDateTime): ZonedDateTime? {
            var date = now.toLocalDate()
            repeat(LOOK_DAYS) {
                if (fallsOn(date)) at(date, now.zone).takeIf { !it.isAfter(now) }?.let { return it }
                date = date.minusDays(1)
            }
            return null
        }

        /** La prossima volta che tocca, dopo [now]; null se non tocca mai. */
        fun nextOccurrence(now: ZonedDateTime): ZonedDateTime? {
            var date = now.toLocalDate()
            repeat(LOOK_DAYS) {
                if (fallsOn(date)) at(date, now.zone).takeIf { it.isAfter(now) }?.let { return it }
                date = date.plusDays(1)
            }
            return null
        }

        private companion object {
            /** Due mesi e un po': basta a trovare il giorno di ogni mese. */
            const val LOOK_DAYS = 70
        }
    }
}
