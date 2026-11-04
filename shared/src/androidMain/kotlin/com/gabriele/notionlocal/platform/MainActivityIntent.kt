package com.gabriele.notionlocal.platform

import android.content.Context
import android.content.Intent

/**
 * **L'attività principale dell'app, per nome** (dal 04/11/2026). `MainActivity`
 * sta nel modulo `app`, e il codice qui (notifiche, registrazione, impostazioni)
 * non la vede: la si chiama col suo nome completo, che per Android è la stessa
 * cosa di `Intent(context, MainActivity::class.java)`.
 */
const val MAIN_ACTIVITY = "com.gabriele.notionlocal.MainActivity"

fun mainActivityIntent(context: Context): Intent = Intent().setClassName(context, MAIN_ACTIVITY)
