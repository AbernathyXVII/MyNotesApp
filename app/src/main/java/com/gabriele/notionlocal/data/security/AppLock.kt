package com.gabriele.notionlocal.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * **La password delle pagine bloccate** (dal 26/09/2026), come quella di
 * Samsung Notes: **una sola per tutta l'app**, scelta la prima volta che si
 * blocca una pagina, e al suo posto l'impronta, se la si vuole.
 *
 * Non si salva la password, ma solo quello che se ne ricava con PBKDF2 e un
 * sale a caso (`PasswordHash`): chi leggesse le preferenze non la
 * ritroverebbe.
 *
 * **Quali pagine sono aperte adesso** (`unlocked`): sbloccata una pagina,
 * resta aperta — con le sue sottopagine — finché si gira al suo interno.
 * Si richiude andando su una pagina che non le appartiene
 * (`keepOnlyUnlocked`) o uscendo dall'app per più di `BACKGROUND_GRACE_MS`:
 * così scegliere un'immagine dalla galleria non la richiude, lasciare il
 * telefono sì.
 */
object AppLock {

    private lateinit var prefs: SharedPreferences

    private var hashState by mutableStateOf<String?>(null)
    private var biometricState by mutableStateOf(true)
    private var unlockedState by mutableStateOf<Set<String>>(emptySet())

    /** Cresce a ogni pagina bloccata o sbloccata: chi guarda se una pagina è protetta rilegge. */
    var protectionVersion by mutableIntStateOf(0)
        private set

    private var stoppedAt = 0L

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
        hashState = prefs.getString(KEY_HASH, null)
        biometricState = prefs.getBoolean(KEY_BIOMETRIC, true)
    }

    val hasPassword: Boolean get() = hashState != null

    /** Se si può aprire con l'impronta (quando il telefono ce l'ha). */
    var biometricEnabled: Boolean
        get() = biometricState
        set(value) {
            biometricState = value
            prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()
        }

    fun setPassword(password: String) {
        val stored = PasswordHash.create(password)
        hashState = stored
        prefs.edit().putString(KEY_HASH, stored).apply()
    }

    fun checkPassword(password: String): Boolean =
        hashState?.let { PasswordHash.verify(password, it) } ?: false

    /** Toglie la password: solo quando nessuna pagina è più bloccata (lo controlla chi chiama). */
    fun removePassword() {
        hashState = null
        prefs.edit().remove(KEY_HASH).apply()
    }

    fun isUnlocked(pageId: String): Boolean = pageId in unlockedState

    fun unlock(pageId: String) {
        unlockedState = unlockedState + pageId
    }

    fun lock(pageId: String) {
        unlockedState = unlockedState - pageId
    }

    /** Si è andati su una pagina che appartiene a `pageId` (o a nessuna, se null): le altre si richiudono. */
    fun keepOnlyUnlocked(pageId: String?) {
        val kept = if (pageId != null && pageId in unlockedState) setOf(pageId) else emptySet()
        if (kept != unlockedState) unlockedState = kept
    }

    fun protectionChanged() {
        protectionVersion++
    }

    fun onAppStopped(now: Long) {
        stoppedAt = now
    }

    fun onAppStarted(now: Long) {
        if (stoppedAt > 0 && now - stoppedAt > BACKGROUND_GRACE_MS) unlockedState = emptySet()
        stoppedAt = 0
    }

    private const val KEY_HASH = "passwordHash"
    private const val KEY_BIOMETRIC = "biometric"
    private const val BACKGROUND_GRACE_MS = 30_000L
}

/**
 * La password trasformata per essere salvata: `"sale:derivata"`, tutti e due
 * in Base64. PBKDF2 con SHA-256 e molti giri, perché provare le password una
 * per una costi tempo; il confronto è a tempo costante.
 */
object PasswordHash {

    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun create(password: String, salt: ByteArray = randomSalt()): String {
        val derived = derive(password, salt)
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(salt) + ":" + encoder.encodeToString(derived)
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 2) return false
        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(parts[0]) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(parts[1]) }.getOrNull() ?: return false
        return MessageDigest.isEqual(derive(password, salt), expected)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun randomSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }
}
