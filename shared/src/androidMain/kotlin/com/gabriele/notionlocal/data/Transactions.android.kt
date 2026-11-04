package com.gabriele.notionlocal.data

import androidx.room.RoomDatabase
import androidx.room.withTransaction as roomWithTransaction

/** Sul telefono: il `withTransaction` di Room per Android, come prima. */
actual suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R = roomWithTransaction(block)
