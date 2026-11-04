package com.gabriele.notionlocal.data.proofing

import com.gabriele.notionlocal.data.settings.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.platform.openAppAsset
import java.io.FileNotFoundException

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

    suspend fun get(context: PlatformContext, language: ProofingLanguage): SpellDictionary? {
        val dir = language.assetDir ?: return null
        return mutex.withLock {
            loaded[language]?.let { return@withLock it }
            fun open(name: String) = openAppAsset(context, "proofing/$dir/$name") ?: throw FileNotFoundException("proofing/$dir/$name")
            val dictionary = withContext(Dispatchers.Default) {
                open("index.aff").bufferedReader(Charsets.UTF_8).use { aff ->
                    open("index.dic").bufferedReader(Charsets.UTF_8).use { dic ->
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
