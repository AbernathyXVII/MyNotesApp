package com.gabriele.notionlocal.data

import androidx.room.RoomDatabase
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/**
 * Sul PC: la connessione di scrittura di Room, con una transazione
 * `IMMEDIATE`. Le chiamate ai DAO dentro [block] usano la stessa connessione,
 * e una transazione dentro un'altra diventa un punto di ripristino della prima.
 */
actual suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R =
    useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
