package com.gabriele.notionlocal.data

import androidx.room.RoomDatabase

/**
 * **Più scritture come una sola** (dal 04/11/2026, nel codice comune): o
 * vanno tutte, o nessuna. Sul telefono è proprio il `withTransaction` di Room
 * per Android, quello di sempre; sul PC, dove quello non c'è, la connessione
 * di scrittura di Room con una transazione `IMMEDIATE`. Chi lo usa importa
 * questo al posto di `androidx.room.withTransaction`.
 */
expect suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R
