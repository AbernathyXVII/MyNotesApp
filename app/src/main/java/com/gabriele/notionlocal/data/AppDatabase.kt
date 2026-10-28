package com.gabriele.notionlocal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabriele.notionlocal.data.dao.BlockDao
import com.gabriele.notionlocal.data.dao.DatabaseDao
import com.gabriele.notionlocal.data.dao.InkDao
import com.gabriele.notionlocal.data.dao.NotebookDao
import com.gabriele.notionlocal.data.dao.PageDao
import com.gabriele.notionlocal.data.dao.PersonalWordDao
import com.gabriele.notionlocal.data.dao.PageEditDao
import com.gabriele.notionlocal.data.dao.TableCellDao
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.NotebookEntity
import com.gabriele.notionlocal.data.entity.NotebookSectionEntity
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PersonalWordEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.encodeSortRules
import com.gabriele.notionlocal.data.entity.encodeGroupRules
import com.gabriele.notionlocal.data.entity.legacyGroupRules
import com.gabriele.notionlocal.data.entity.parseDateGroupings
import com.gabriele.notionlocal.data.entity.TableCellEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Il database SQLite locale dell'app. Questo è letteralmente "tutto
 * salvato sul telefono": nessuna chiamata di rete, nessun account,
 * nessun cloud. Il file .db vive dentro la sandbox dell'app su disco.
 *
 * **Da qui in avanti ogni cambio di schema vuole una migrazione vera.**
 * Fino alla versione 5 il database usava fallbackToDestructiveMigration,
 * cioè si ricreava da zero ad ogni cambio di schema, perdendo tutto: una
 * scelta deliberata finché sul telefono c'erano solo dati di prova. Ora
 * che ci sono note vere quella scorciatoia è stata tolta. Se si aggiunge
 * un campo senza scrivere la migrazione corrispondente, l'app si rifiuta
 * di partire — ed è quello che vogliamo: un errore rumoroso è molto
 * meglio delle note cancellate in silenzio.
 */
@Database(
    entities = [
        PageEntity::class,
        BlockEntity::class,
        DatabaseColumnEntity::class,
        DatabaseRowEntity::class,
        DatabaseCellEntity::class,
        TableCellEntity::class,
        PageEditEntity::class,
        InkStrokeEntity::class,
        NotebookEntity::class,
        NotebookSectionEntity::class,
        PersonalWordEntity::class
    ],
    version = 67,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pageDao(): PageDao
    abstract fun pageEditDao(): PageEditDao
    abstract fun blockDao(): BlockDao
    abstract fun databaseDao(): DatabaseDao
    abstract fun tableCellDao(): TableCellDao
    abstract fun inkDao(): InkDao
    abstract fun notebookDao(): NotebookDao
    abstract fun personalWordDao(): PersonalWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Aggiunge `numberStartsAt` ai blocchi: serve agli elenchi
         * numerati che partono da un numero scelto (scrivendo "5." a
         * inizio blocco). Travasa anche il vecchio `numberResetHere`,
         * che diceva solo "riparti da 1", nel caso equivalente
         * `numberStartsAt = 1`.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocks ADD COLUMN numberStartsAt INTEGER")
                db.execSQL("UPDATE blocks SET numberStartsAt = 1 WHERE numberResetHere = 1")
            }
        }

        /**
         * Aggiunge `updatedAt` alle righe dei database, per la proprietà
         * "Last edited time". Ammette null: le righe già esistenti non
         * hanno una storia da cui ricavare quando sono state toccate
         * l'ultima volta, e inventare una data sarebbe peggio che dire
         * "non lo so" e ripiegare sulla data di creazione.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_rows ADD COLUMN updatedAt INTEGER")
            }
        }

        /**
         * Aggiunge `databaseLayout` alle pagine: come una pagina-database
         * mostra le sue righe. Ammette null, che vale "tabella" — i
         * database già esistenti stavano di fatto usando quella.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN databaseLayout TEXT")
            }
        }

        /**
         * Aggiunge `boardGroupColumnId` alle pagine: quale proprietà fa
         * da colonna nella vista a bacheca. Ammette null, che vale
         * "scegli tu la prima adatta".
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN boardGroupColumnId TEXT")
            }
        }

        /**
         * Aggiunge `calendarDateColumnId` alle pagine: quale proprietà
         * data colloca le righe nella vista calendario. Ammette null,
         * che vale "scegli tu la prima adatta".
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN calendarDateColumnId TEXT")
            }
        }

        /**
         * Aggiunge `calendarWeekView` alle pagine: se il calendario
         * mostra una settimana invece di un mese. Ammette null, che
         * vale "mese".
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN calendarWeekView INTEGER")
            }
        }

        /**
         * Aggiunge `calendarMode` alle pagine: anno, mese o settimana.
         * Il vecchio `calendarWeekView` era un sì/no e non poteva
         * reggere una terza scelta, quindi il suo valore viene
         * travasato qui. La colonna vecchia resta dov'è: SQLite sa
         * cancellare una colonna solo dalla 3.35, più recente del
         * minimo che l'app sostiene, e ricreare la tabella delle pagine
         * per togliere un campo inutilizzato è un rischio che non vale
         * il guadagno.
         */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN calendarMode TEXT")
                db.execSQL("UPDATE pages SET calendarMode = 'WEEK' WHERE calendarWeekView = 1")
                db.execSQL("UPDATE pages SET calendarMode = 'MONTH' WHERE calendarWeekView = 0")
            }
        }

        /**
         * Aggiunge `timelineZoom` alle pagine: quanto tempo sta in una
         * schermata della linea del tempo. Ammette null, che vale
         * "giorno" — l'unico ingrandimento che esisteva prima.
         */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN timelineZoom TEXT")
            }
        }

        /**
         * Aggiunge `iconImage` e `coverImage` alle pagine: l'immagine
         * scelta dall'utente al posto dell'emoji, e la copertina sopra
         * il titolo. Ammettono null, che vale "nessuna immagine" — cioè
         * quello che avevano tutte le pagine fino a ieri.
         */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN iconImage TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN coverImage TEXT")
            }
        }

        /**
         * Aggiunge `sortColumnId` e `sortDescending` alle pagine: in
         * base a quale proprietà sono ordinate le righe e in che
         * verso. `sortColumnId` ammette null, che vale "nell'ordine in
         * cui sono state create" — come stavano prima. `sortDescending`
         * non ammette null e vuole quindi un valore per le righe già
         * esistenti: crescente, che è il verso di partenza.
         */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN sortColumnId TEXT")
                db.execSQL(
                    "ALTER TABLE pages ADD COLUMN sortDescending INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Aggiunge `showEmbeddedTitle` alle pagine: se il titolo del
         * database si vede quando è dentro una pagina. Non ammette
         * null e parte da 1 — mostrato — perché è come stavano tutti i
         * database prima che la scelta esistesse.
         */
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pages ADD COLUMN showEmbeddedTitle INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /**
         * Aggiunge `coverScale`, `coverOffsetX` e `coverOffsetY` alle
         * pagine: come è inquadrata la copertina dentro la sua
         * striscia. Non ammettono null e partono da 1 e 0 — immagine
         * centrata e non ingrandita, cioè esattamente com'era prima
         * che si potesse spostarla.
         */
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN coverScale REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE pages ADD COLUMN coverOffsetX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE pages ADD COLUMN coverOffsetY REAL NOT NULL DEFAULT 0.0")
            }
        }

        /**
         * Aggiunge `tableGroupColumnId` e `hideEmptyGroups` alle
         * pagine: in base a quale proprietà la tabella raggruppa le
         * righe, e se i gruppi vuoti si nascondono. Il primo ammette
         * null, che vale "nessun raggruppamento" — come stavano tutte
         * le tabelle finora. Il secondo no e parte da 1, acceso.
         */
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN tableGroupColumnId TEXT")
                db.execSQL(
                    "ALTER TABLE pages ADD COLUMN hideEmptyGroups INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /**
         * Aggiunge `hidden` alle colonne dei database: se la colonna si
         * vede nella tabella. Non ammette null e parte da 0 — visibile
         * — perché è come stavano tutte le colonne prima che si
         * potessero nascondere.
         */
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE database_columns ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Aggiunge `filterColumnId` e `filterValue` alle pagine: in
         * base a quale proprietà sono filtrate le righe e su quale
         * valore. Ammettono null, che vale "nessun filtro" — cioè
         * tutte le pagine, come stavano finora.
         */
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN filterColumnId TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN filterValue TEXT")
            }
        }

        /**
         * Aggiunge `centerContent` alle colonne: se il contenuto della
         * cella sta al centro invece che a sinistra. Non ammette null
         * e parte da 0 — a sinistra — perché è come stavano tutte le
         * colonne prima che si potesse scegliere.
         */
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE database_columns ADD COLUMN centerContent " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Aggiunge `isFavorite` alle pagine. Vale anche per i database,
         * che qui sono pagine con `isDatabase = 1`. Non ammette null e
         * parte da 0: prima che i preferiti esistessero non lo era
         * nessuna.
         */
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pages ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Il menu delle opzioni di una pagina: blocco del contenuto,
         * blocco della vista, cestino, e la tabella della cronologia
         * che sta dietro alla voce "Updates".
         *
         * La tabella si crea qui a mano e **deve corrispondere esatta**
         * a quella che Room si aspetta da `PageEditEntity`, indice
         * compreso: se differisce anche solo per il nome dell'indice,
         * all'avvio successivo Room se ne accorge e rifiuta di partire.
         */
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pages ADD COLUMN isViewLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pages ADD COLUMN trashedAt INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `page_edits` (" +
                        "`id` TEXT NOT NULL, " +
                        "`pageId` TEXT NOT NULL, " +
                        "`blockId` TEXT NOT NULL, " +
                        "`at` INTEGER NOT NULL, " +
                        "`before` TEXT NOT NULL, " +
                        "`after` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`pageId`) REFERENCES `pages`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_page_edits_pageId` ON `page_edits` (`pageId`)")
            }
        }

        /**
         * Aggiunge `favoritedAt` alle pagine: quando una pagina è stata
         * messa fra i preferiti. Ammette null, e i preferiti che c'erano
         * già restano senza data: quella vera non si può più sapere.
         */
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN favoritedAt INTEGER")
            }
        }

        /**
         * Aggiunge `galleryCardPreview` e `galleryCardSize` alle pagine:
         * cosa mostrano le schede della vista a galleria e quanto sono
         * grandi. Ammettono null, che vale "copertina" e "media" — la
         * galleria prima non esisteva, quindi non c'è una scelta
         * precedente da rispettare.
         */
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN galleryCardPreview TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN galleryCardSize TEXT")
            }
        }

        /**
         * Aggiunge `pageFont` e `pageFontSize` alle pagine: il font e il
         * corpo del testo scelti dalla barra Aa. Ammettono null, che vale
         * "come prima" — il font di sistema e il corpo 16 — cioè
         * esattamente come si vedevano tutte le pagine finora.
         */
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN pageFont TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN pageFontSize INTEGER")
            }
        }

        /**
         * **Toglie i titoli (Heading 1, 2 e 3)**, su richiesta dell'utente:
         * ora il corpo del testo si sceglie per pagina dalla barra Aa, e
         * titoli a misura fissa non avevano più senso. Quelli già scritti
         * diventano **paragrafi normali**, senza grassetto — scelta
         * dell'utente fra le tre proposte (grassetto, normale, lasciarli).
         * Il testo non si tocca: cambia solo il tipo del blocco, e
         * formattazione e colori messi a mano restano.
         *
         * Non cambia la forma di nessuna tabella, solo dei valori: Room
         * salva i tipi per nome, e dall'enum `BlockType` i tre nomi sono
         * stati tolti. **Questa migrazione deve girare prima di qualunque
         * lettura**, altrimenti un blocco con un nome che l'enum non
         * conosce più farebbe chiudere l'app — ed è quello che Room
         * garantisce, facendo le migrazioni all'apertura del database.
         */
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE blocks SET type = 'PARAGRAPH' " +
                        "WHERE type IN ('HEADING_1', 'HEADING_2', 'HEADING_3')"
                )
            }
        }

        /**
         * Aggiunge `isSimpleDatabase` alle pagine: il database le cui righe
         * sono solo testo e non diventano mai pagine. Non ammette null e
         * parte da 0 — database normale — perché è quello che erano tutti
         * i database creati finora.
         */
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pages ADD COLUMN isSimpleDatabase INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Le viste collegate ("Linked view of data source"): quale database
         * mostrano, e quali proprietà nascondono solo loro. Due colonne
         * vuote per tutte le pagine che c'erano: nessuna era una vista.
         */
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN sourceDatabaseId TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN viewHiddenColumnIds TEXT")
            }
        }

        /**
         * Due cose del 25/09/2026, in una migrazione sola:
         *  - **via MS YaHei** (chiesto dall'utente, al suo posto SimSun): le
         *    pagine che lo usavano tornano al font di sistema, che sul
         *    cinese è lo stesso disegno. Va fatto prima di qualunque
         *    lettura: Room legge il font per nome, e "MS_YAHEI" non esiste
         *    più nell'enum;
         *  - **l'inquadratura della copertina nelle schede della galleria**:
         *    tre colonne vuote, cioè "la stessa della pagina", com'era.
         */
        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE pages SET pageFont = NULL WHERE pageFont = 'MS_YAHEI'")
                db.execSQL("ALTER TABLE pages ADD COLUMN cardCoverScale REAL")
                db.execSQL("ALTER TABLE pages ADD COLUMN cardCoverOffsetX REAL")
                db.execSQL("ALTER TABLE pages ADD COLUMN cardCoverOffsetY REAL")
            }
        }

        /**
         * **Font e corpo passano dalla pagina al testo** (25/09/2026,
         * chiesto dall'utente: selezionare un pezzo di testo e cambiare il
         * font solo di quello). Le pagine che avevano un font o un corpo
         * "di pagina" li danno a ogni pezzo di testo dei loro blocchi che
         * non ne ha già uno suo, così si vedono esattamente come prima;
         * poi le due colonne della pagina si svuotano, e nessuno le scrive
         * più. Le celle delle tabelle semplici sono testo senza
         * formattazione: lì il font di pagina si perde, e tornano al font
         * di sistema. Nessun cambio di forma alle tabelle.
         *
         * Il corpo 16 è quello di sempre, e nel testo si scrive come
         * "nessun corpo" (`DEFAULT_SPAN_SIZE`).
         */
        private const val DEFAULT_SPAN_SIZE = 16

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val json = Json { ignoreUnknownKeys = true }
                val pages = mutableListOf<Triple<String, String?, Int?>>()
                db.query("SELECT id, pageFont, pageFontSize FROM pages WHERE pageFont IS NOT NULL OR pageFontSize IS NOT NULL")
                    .use { c ->
                        while (c.moveToNext()) {
                            pages += Triple(
                                c.getString(0),
                                if (c.isNull(1)) null else c.getString(1),
                                if (c.isNull(2)) null else c.getInt(2)
                            )
                        }
                    }
                for ((pageId, font, size) in pages) {
                    val blocks = mutableListOf<Pair<String, String>>()
                    db.query("SELECT id, textJson FROM blocks WHERE pageId = ?", arrayOf<Any?>(pageId)).use { c ->
                        while (c.moveToNext()) blocks += c.getString(0) to c.getString(1)
                    }
                    for ((blockId, textJson) in blocks) {
                        val spans = runCatching { json.decodeFromString<List<RichTextSpan>>(textJson) }.getOrNull() ?: continue
                        val moved = spans.map { span ->
                            if (span.text.isEmpty()) {
                                span
                            } else {
                                span.copy(
                                    font = span.font ?: font,
                                    size = span.size ?: size?.takeIf { it != DEFAULT_SPAN_SIZE }
                                )
                            }
                        }
                        if (moved != spans) {
                            db.execSQL("UPDATE blocks SET textJson = ? WHERE id = ?", arrayOf<Any?>(json.encodeToString(moved), blockId))
                        }
                    }
                }
                db.execSQL("UPDATE pages SET pageFont = NULL, pageFontSize = NULL")
            }
        }

        /**
         * **L'ora delle notifiche** (25/09/2026): la colonna `notifyAt` per
         * le righe fatte con "/notification". Vuota per tutte le righe che
         * c'erano, che non sono notifiche.
         */
        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocks ADD COLUMN notifyAt INTEGER")
            }
        }

        /**
         * **I disegni a mano** (25/09/2026): la tabella `ink_strokes`, un
         * tratto per riga, attaccato a una pagina (e con lei cancellato).
         * Vedi `InkStrokeEntity`. Nessun dato da spostare: prima non si
         * disegnava.
         */
        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ink_strokes` (`id` TEXT NOT NULL, `pageId` TEXT NOT NULL, " +
                        "`anchor` TEXT NOT NULL, `tool` TEXT NOT NULL, `color` INTEGER NOT NULL, `width` REAL NOT NULL, " +
                        "`points` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`pageId`) REFERENCES `pages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ink_strokes_pageId` ON `ink_strokes` (`pageId`)")
            }
        }

        /**
         * **Le pagine bloccate con la password** (26/09/2026): la colonna
         * `isProtected`, spenta per tutte le pagine che c'erano.
         */
        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN isProtected INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * **I quaderni** (26/09/2026), come quelli di OneNote: le tabelle dei
         * quaderni e delle loro sezioni, vuote, e per le pagine la sezione in
         * cui stanno — null per tutte quelle che c'erano, che restano del
         * menu principale.
         */
        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notebooks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`color` INTEGER NOT NULL, `iconImage` TEXT, `orderIndex` INTEGER NOT NULL, " +
                        "`isExpanded` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notebook_sections` (`id` TEXT NOT NULL, `notebookId` TEXT NOT NULL, " +
                        "`parentId` TEXT, `isGroup` INTEGER NOT NULL, `title` TEXT NOT NULL, `color` INTEGER NOT NULL, " +
                        "`orderIndex` INTEGER NOT NULL, `isExpanded` INTEGER NOT NULL, `isProtected` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`notebookId`) REFERENCES " +
                        "`notebooks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notebook_sections_notebookId` ON `notebook_sections` (`notebookId`)"
                )
                db.execSQL("ALTER TABLE pages ADD COLUMN notebookSectionId TEXT")
            }
        }

        /**
         * **Le colonne Number e i calcoli** (26/09/2026): formato, cifre
         * decimali, barra o anello, colore, numero accanto, anello al centro;
         * e per ogni vista i calcoli sotto la tabella. Tutto vuoto per quello
         * che c'era: i numeri si vedono come prima.
         */
        private val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_columns ADD COLUMN numberFormat TEXT")
                db.execSQL("ALTER TABLE database_columns ADD COLUMN decimalPlaces INTEGER")
                db.execSQL("ALTER TABLE database_columns ADD COLUMN numberDisplay TEXT")
                db.execSQL("ALTER TABLE database_columns ADD COLUMN numberColor TEXT")
                db.execSQL("ALTER TABLE database_columns ADD COLUMN showNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE database_columns ADD COLUMN ringCentered INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pages ADD COLUMN viewCalculations TEXT")
            }
        }

        /**
         * **Le celle a sinistra, al centro o a destra** (26/09/2026): la
         * colonna `contentAlignment`, vuota per tutte. Le caselle già
         * centrate col vecchio interruttore restano al centro (vedi
         * `cellAlignment`).
         */
        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_columns ADD COLUMN contentAlignment TEXT")
            }
        }

        /**
         * **Il formato della data e dell'ora delle colonne Date**
         * (26/09/2026): vuoti per tutte, e le date si leggono come prima.
         */
        private val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_columns ADD COLUMN dateFormat TEXT")
                db.execSQL("ALTER TABLE database_columns ADD COLUMN timeFormat TEXT")
            }
        }

        /**
         * 38 → 39 (26/09/2026): i template delle pagine, come su Notion
         * (`PageEntity.isTemplate`). Nessuna pagina di prima lo è.
         */
        private val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 39 → 40 (28/09/2026): la vista Chart dei database, con le sue
         * impostazioni (`PageEntity.chartSettings`). Nessuna vista di prima
         * è un grafico.
         */
        private val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN chartSettings TEXT")
            }
        }

        /**
         * 40 → 41 (29/09/2026): i sottogruppi della tabella
         * (`PageEntity.tableSubGroupColumnId`). Nessuna vista di prima li ha.
         */
        private val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN tableSubGroupColumnId TEXT")
            }
        }

        /**
         * 41 → 42 (29/09/2026): "Hide empty sub groups", separato da "Hide
         * empty groups" (`PageEntity.hideEmptySubGroups`). Parte uguale a
         * quello dei gruppi, che fino a qui valeva per tutti e due: nessuna
         * tabella cambia aspetto da sola.
         */
        private val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN hideEmptySubGroups INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE pages SET hideEmptySubGroups = hideEmptyGroups")
            }
        }

        /**
         * 42 → 43 (29/09/2026): le viste di un database, come su Notion
         * (`PageEntity.viewHostId`, `viewName`, `activeViewId`). Nessun
         * database ne ha ancora: ognuno ha la sua, quella di sempre.
         */
        private val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN viewHostId TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN viewName TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN activeViewId TEXT")
            }
        }

        /**
         * 43 → 44 (29/09/2026): le larghezze delle colonne della tabella
         * (`PageEntity.columnWidths`, `fitColumns`). Nessuna colonna ne ha
         * una sua: tutte restano larghe com'erano.
         */
        private val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN columnWidths TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN fitColumns TEXT")
            }
        }

        /**
         * 44 → 45 (29/09/2026): le sotto-righe (`DatabaseRowEntity.parentRowId`,
         * `subRowsCollapsed`). Nessuna riga ne ha: tutte restano principali.
         */
        private val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_rows ADD COLUMN parentRowId TEXT")
                db.execSQL("ALTER TABLE database_rows ADD COLUMN subRowsCollapsed INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 45 → 46 (29/09/2026): la colonna Name si sposta
         * (`PageEntity.nameColumnOrder`). Tutti i database la tengono dov'era,
         * la prima.
         */
        private val MIGRATION_45_46 = object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN nameColumnOrder INTEGER NOT NULL DEFAULT -1")
            }
        }

        /**
         * 46 → 47 (29/09/2026): la proprietà "Duration" e fino a quale unità
         * si scrive (`DatabaseColumnEntity.durationFormat`). Le colonne che
         * ci sono non la usano.
         */
        private val MIGRATION_46_47 = object : Migration(46, 47) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_columns ADD COLUMN durationFormat TEXT")
            }
        }

        /**
         * 47 → 48 (30/09/2026): il testo lungo nelle celle, nelle impostazioni
         * di una tabella (`PageEntity.cellOverflow`, colonna `wrapCells`). A 0
         * dappertutto: le celle restano su una riga, coi tre puntini.
         */
        private val MIGRATION_47_48 = object : Migration(47, 48) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN wrapCells INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 48 → 49 (30/09/2026): "Color cards" della bacheca
         * (`PageEntity.colorBoardCards`). Spento dappertutto: le schede
         * restano del colore di sempre.
         */
        private val MIGRATION_48_49 = object : Migration(48, 49) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN colorBoardCards INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 49 → 50 (30/09/2026): il colore degli eventi del calendario
         * (`DatabaseRowEntity.eventColor`), per il time-blocking. Nessuna
         * riga ne ha: restano del colore di sempre.
         */
        private val MIGRATION_49_50 = object : Migration(49, 50) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_rows ADD COLUMN eventColor TEXT")
            }
        }

        /**
         * 50 → 51 (05/10/2026): la seconda regola di ordinamento
         * (`PageEntity.thenSortColumnId` e `thenSortDescending`). Nessun
         * database ne ha una: restano ordinati come prima.
         */
        private val MIGRATION_50_51 = object : Migration(50, 51) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN thenSortColumnId TEXT")
                db.execSQL("ALTER TABLE pages ADD COLUMN thenSortDescending INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 51 → 52 (06/10/2026): le voci in più dei blocchi nuovi — riquadro,
         * citazione, codice, immagine, video, audio, file, segnalibro, link a
         * una pagina — in `BlockEntity.attrsJson` (vedi `BlockAttrs`). I
         * blocchi che c'erano non ne hanno: "{}".
         */
        private val MIGRATION_51_52 = object : Migration(51, 52) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocks ADD COLUMN attrsJson TEXT NOT NULL DEFAULT '{}'")
            }
        }

        /**
         * Le regole di ordinamento, quante se ne vogliono (17/10/2026): tutte
         * in un elenco solo, `pages.sortRulesJson`. La prima e la seconda
         * regola di ogni pagina ci passano dentro, nello stesso ordine e
         * verso, e i loro campi di prima si svuotano.
         */
        private val MIGRATION_56_57 = object : Migration(56, 57) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN sortRulesJson TEXT DEFAULT NULL")
                val moved = mutableListOf<Pair<String, String>>()
                db.query(
                    "SELECT id, sortColumnId, sortDescending, thenSortColumnId, thenSortDescending " +
                        "FROM pages WHERE sortColumnId IS NOT NULL"
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val rules = buildList {
                            add(SortRuleSpec(cursor.getString(1), cursor.getInt(2) != 0))
                            if (!cursor.isNull(3)) add(SortRuleSpec(cursor.getString(3), cursor.getInt(4) != 0))
                        }
                        encodeSortRules(rules)?.let { moved += cursor.getString(0) to it }
                    }
                }
                moved.forEach { (id, json) ->
                    db.execSQL("UPDATE pages SET sortRulesJson = ? WHERE id = ?", arrayOf<Any>(json, id))
                }
                db.execSQL(
                    "UPDATE pages SET sortColumnId = NULL, sortDescending = 0, " +
                        "thenSortColumnId = NULL, thenSortDescending = 0"
                )
            }
        }

        /**
         * I raggruppamenti, quanti se ne vogliono (28/10/2026): tutti in un
         * elenco solo per la tabella e uno per la bacheca. Il gruppo e il
         * sottogruppo di ogni vista ci passano dentro, coi loro "Hide empty
         * groups" e "Date by", e i loro campi di prima si svuotano. Una bacheca
         * con le fasce ma senza una proprietà scelta per le colonne usava la
         * prima a selezione singola: diventa la sua prima regola.
         */
        private val MIGRATION_65_66 = object : Migration(65, 66) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN tableGroupRulesJson TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pages ADD COLUMN boardGroupRulesJson TEXT DEFAULT NULL")
                val moved = mutableListOf<Triple<String, String?, String?>>()
                db.query(
                    "SELECT id, sourceDatabaseId, tableGroupColumnId, tableSubGroupColumnId, hideEmptyGroups, " +
                        "hideEmptySubGroups, boardGroupColumnId, boardSubGroupColumnId, boardHideEmptyGroups, " +
                        "boardHideEmptySubGroups, dateGroupingJson FROM pages " +
                        "WHERE tableGroupColumnId IS NOT NULL OR boardGroupColumnId IS NOT NULL OR boardSubGroupColumnId IS NOT NULL"
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        fun text(i: Int) = if (cursor.isNull(i)) null else cursor.getString(i)
                        val id = cursor.getString(0)
                        val dates = parseDateGroupings(text(10))
                        val table = legacyGroupRules(
                            text(2), text(3), cursor.getInt(4) != 0, cursor.getInt(5) != 0, dates.table, dates.tableSub
                        )
                        val boardSub = text(7)
                        val boardGroup = text(6) ?: boardSub?.let {
                            // Le colonne di prima: la prima proprietà a selezione singola dei dati.
                            db.query(
                                "SELECT id FROM database_columns WHERE pageId = ? AND type = 'SELECT' ORDER BY orderIndex LIMIT 1",
                                arrayOf<Any>(text(1) ?: id)
                            ).use { c -> if (c.moveToFirst()) c.getString(0) else null }
                        }
                        val board = legacyGroupRules(
                            boardGroup, boardSub, cursor.getInt(8) != 0, cursor.getInt(9) != 0, dates.board, dates.boardSub
                        )
                        moved += Triple(id, encodeGroupRules(table), encodeGroupRules(board))
                    }
                }
                moved.forEach { (id, table, board) ->
                    db.execSQL(
                        "UPDATE pages SET tableGroupRulesJson = ?, boardGroupRulesJson = ? WHERE id = ?",
                        arrayOf<Any?>(table, board, id)
                    )
                }
                db.execSQL(
                    "UPDATE pages SET tableGroupColumnId = NULL, tableSubGroupColumnId = NULL, " +
                        "boardGroupColumnId = NULL, boardSubGroupColumnId = NULL, dateGroupingJson = NULL"
                )
            }
        }

        /**
         * Una cella sola per riga e proprietà (28/10/2026). Le doppie nate
         * scrivendo in fretta in una cella vuota se ne vanno: resta la prima,
         * che è quella su cui finivano le modifiche dopo (`getCell` prende la
         * prima); le altre avevano solo le prime lettere. Poi l'indice unico.
         */
        private val MIGRATION_66_67 = object : Migration(66, 67) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // `_rowid_` e non `rowid`: in questa tabella "rowId" è una colonna (la
                // riga del database), e in SQLite copre il numero interno con quel nome.
                db.execSQL(
                    "DELETE FROM database_cells WHERE _rowid_ NOT IN " +
                        "(SELECT MIN(_rowid_) FROM database_cells GROUP BY rowId, columnId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_database_cells_rowId_columnId` " +
                        "ON `database_cells` (`rowId`, `columnId`)"
                )
            }
        }

        /** "Hide icon" delle proprietà (27/10/2026): tutte le icone si vedono, come prima. */
        private val MIGRATION_64_65 = object : Migration(64, 65) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_columns ADD COLUMN hideIcon INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pages ADD COLUMN hideNameIcon INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** "Full or page line" delle pagine (27/10/2026): tutte come Settings. */
        private val MIGRATION_63_64 = object : Migration(63, 64) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN fullLineMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** La scritta delle celle URL (27/10/2026): nessuna, si vede il link come prima. */
        private val MIGRATION_62_63 = object : Migration(62, 63) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE database_cells ADD COLUMN linkLabel TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE database_cells ADD COLUMN linkLabelIsTitle INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** "Hide New" della bacheca (26/10/2026): spento, i "+ New" si vedono come prima. */
        private val MIGRATION_61_62 = object : Migration(61, 62) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN boardHideNew INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** "Hide empty groups" della bacheca (21/10/2026): spento, le colonne vuote si vedono come prima. */
        private val MIGRATION_60_61 = object : Migration(60, 61) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN boardHideEmptyGroups INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** "Date by" dei raggruppamenti per data (21/10/2026): tutti per giorno, com'erano. */
        private val MIGRATION_59_60 = object : Migration(59, 60) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN dateGroupingJson TEXT DEFAULT NULL")
            }
        }

        /** Il "Paragraph" delle pagine (21/10/2026): tutte come Settings. */
        private val MIGRATION_58_59 = object : Migration(58, 59) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN paragraphJson TEXT DEFAULT NULL")
            }
        }

        /** Il nome della proprietà principale dei database (18/10/2026): per tutti "Name", come prima. */
        private val MIGRATION_57_58 = object : Migration(57, 58) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN nameColumnTitle TEXT DEFAULT NULL")
            }
        }

        /**
         * I sottogruppi della bacheca (14/10/2026): nessuna bacheca li ha, e
         * le fasce vuote, quando ci saranno, si nascondono come nella tabella.
         */
        private val MIGRATION_55_56 = object : Migration(55, 56) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN boardSubGroupColumnId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pages ADD COLUMN boardHideEmptySubGroups INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * Il correttore (08/10/2026): le scelte di ogni pagina — tutte a
         * "come dicono le impostazioni" — e il dizionario dell'utente,
         * vuoto.
         */
        private val MIGRATION_54_55 = object : Migration(54, 55) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN proofingMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pages ADD COLUMN proofingLanguage TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pages ADD COLUMN proofingIgnored TEXT DEFAULT NULL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `personal_words` " +
                        "(`word` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`word`))"
                )
            }
        }

        /** Prima colonna e prima riga ferme nella tabella (08/10/2026): spente dappertutto. */
        private val MIGRATION_53_54 = object : Migration(53, 54) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN freezeFirstColumn INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pages ADD COLUMN freezeFirstRow INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Le icone di Material delle pagine (08/10/2026): nessuna pagina ne ha una. */
        private val MIGRATION_52_53 = object : Migration(52, 53) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN iconSymbol TEXT DEFAULT NULL")
            }
        }

        /**
         * Solo per i test: chiude il database e lo dimentica, così il
         * `getInstance` dopo riapre il file da capo (e ne esegue le
         * migrazioni). Senza, il secondo test troverebbe il database del
         * primo, già chiuso.
         */
        internal fun resetForTests() = closeInstance()

        /**
         * Chiude il database e lo dimentica: lo usa anche il ripristino di
         * un backup (dal 28/10/2026), che sostituisce il file prima che
         * l'app lo riapra (vedi `Backups.applyPendingRestore`).
         */
        fun closeInstance() = synchronized(this) {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notion_local.db"
                )
                    .addMigrations(
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_23_24,
                        MIGRATION_24_25,
                        MIGRATION_25_26,
                        MIGRATION_26_27,
                        MIGRATION_27_28,
                        MIGRATION_28_29,
                        MIGRATION_29_30,
                        MIGRATION_30_31,
                        MIGRATION_31_32,
                        MIGRATION_32_33,
                        MIGRATION_33_34,
                        MIGRATION_34_35,
                        MIGRATION_35_36,
                        MIGRATION_36_37,
                        MIGRATION_37_38,
                        MIGRATION_38_39,
                        MIGRATION_39_40,
                        MIGRATION_40_41,
                        MIGRATION_41_42,
                        MIGRATION_42_43,
                        MIGRATION_43_44,
                        MIGRATION_44_45,
                        MIGRATION_45_46,
                        MIGRATION_46_47,
                        MIGRATION_47_48,
                        MIGRATION_48_49,
                        MIGRATION_49_50,
                        MIGRATION_50_51,
                        MIGRATION_51_52,
                        MIGRATION_52_53,
                        MIGRATION_53_54,
                        MIGRATION_54_55,
                        MIGRATION_55_56,
                        MIGRATION_56_57,
                        MIGRATION_57_58,
                        MIGRATION_58_59,
                        MIGRATION_59_60,
                        MIGRATION_60_61,
                        MIGRATION_61_62,
                        MIGRATION_62_63,
                        MIGRATION_63_64,
                        MIGRATION_64_65,
                        MIGRATION_65_66,
                        MIGRATION_66_67
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
