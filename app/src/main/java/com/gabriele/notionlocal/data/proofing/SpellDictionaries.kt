package com.gabriele.notionlocal.data.proofing

import android.content.Context
import com.gabriele.notionlocal.data.settings.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * I dizionari letti. Leggerne uno prende qualche decimo di secondo e una
 * decina di MB: lo si fa la prima volta che serve, fuori dal filo
 * principale, e poi lo si tiene. Al più due lingue alla volta — quella di
 * sempre e quella di una pagina scritta in un'altra lingua.
 */
object SpellDictionaries {

    private const val KEEP = 2

    private val mutex = Mutex()
    private val loaded = LinkedHashMap<ProofingLanguage, SpellDictionary>()

    suspend fun get(context: Context, language: ProofingLanguage): SpellDictionary? {
        val dir = language.assetDir ?: return null
        return mutex.withLock {
            loaded[language]?.let { return@withLock it }
            val assets = context.applicationContext.assets
            val dictionary = withContext(Dispatchers.Default) {
                assets.open("proofing/$dir/index.aff").bufferedReader(Charsets.UTF_8).use { aff ->
                    assets.open("proofing/$dir/index.dic").bufferedReader(Charsets.UTF_8).use { dic ->
                        SpellDictionary.read(aff, dic, language.locale)
                    }
                }
            }
            loaded[language] = dictionary
            while (loaded.size > KEEP) loaded.remove(loaded.keys.first())
            dictionary
        }
    }
}
