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
import com.gabriele.notionlocal.data.entity.RichTextSpan
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
        NotebookSectionEntity::class
    ],
    version = 39,
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
         * Solo per i test: chiude il database e lo dimentica, così il
         * `getInstance` dopo riapre il file da capo (e ne esegue le
         * migrazioni). Senza, il secondo test troverebbe il database del
         * primo, già chiuso.
         */
        internal fun resetForTests() = synchronized(this) {
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
                        MIGRATION_38_39
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
