package com.gabriele.notionlocal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabriele.notionlocal.data.dao.BlockDao
import com.gabriele.notionlocal.data.dao.DatabaseDao
import com.gabriele.notionlocal.data.dao.PageDao
import com.gabriele.notionlocal.data.dao.PageEditDao
import com.gabriele.notionlocal.data.dao.TableCellDao
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.TableCellEntity

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
        PageEditEntity::class
    ],
    version = 26,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pageDao(): PageDao
    abstract fun pageEditDao(): PageEditDao
    abstract fun blockDao(): BlockDao
    abstract fun databaseDao(): DatabaseDao
    abstract fun tableCellDao(): TableCellDao

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
                        MIGRATION_25_26
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
