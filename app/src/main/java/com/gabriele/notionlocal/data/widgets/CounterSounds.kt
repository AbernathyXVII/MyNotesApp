package com.gabriele.notionlocal.data.widgets

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * **I suoni del contatore** (dal 18/10/2026, chiesto dall'utente): un click
 * su +, uno più lieve su −, e qualche alternativa da scegliere. Non vengono
 * da file: l'app li disegna da sé, onda per onda (`samples`), e li suona come
 * i suoni dei tasti — col telefono in silenzioso non si sentono.
 */
enum class CounterSound {
    CLICK, SOFT_CLICK, TICK, POP, BUBBLE, WOOD, BEEP, COIN;

    companion object {
        fun of(name: String?): CounterSound = entries.find { it.name == name } ?: CLICK
    }
}

const val COUNTER_SAMPLE_RATE = 44_100

object CounterSounds {

    /**
     * L'onda di un suono, a 44,1 kHz, 16 bit, mono. Sempre la stessa per lo
     * stesso suono (il fruscio del click viene da un caso col seme fisso).
     */
    fun samples(sound: CounterSound): ShortArray {
        val wave: (Double) -> Double
        val seconds: Double
        when (sound) {
            // Un colpo secco: un po' di fruscio e una nota acuta che si spengono subito.
            CounterSound.CLICK -> {
                seconds = 0.03
                val noise = noise(seconds)
                wave = { t -> 0.9 * exp(-t / 0.003) * (0.55 * sin(2 * PI * 1800 * t) + 0.45 * noise(t)) }
            }
            // Lo stesso più basso e più piano: per il meno.
            CounterSound.SOFT_CLICK -> {
                seconds = 0.03
                val noise = noise(seconds, seed = 7)
                wave = { t -> 0.32 * exp(-t / 0.004) * (0.75 * sin(2 * PI * 1100 * t) + 0.25 * noise(t)) }
            }
            // Il ticchettio di un orologio.
            CounterSound.TICK -> {
                seconds = 0.02
                wave = { t -> 0.6 * exp(-t / 0.0018) * sin(2 * PI * 3200 * t) }
            }
            // Una bolla che scoppia: la nota scende.
            CounterSound.POP -> {
                seconds = 0.06
                wave = { t -> 0.7 * exp(-t / 0.015) * sin(2 * PI * (900 * t - 6_500 * t * t)) }
            }
            // Una goccia: la nota sale.
            CounterSound.BUBBLE -> {
                seconds = 0.07
                wave = { t -> 0.5 * exp(-t / 0.02) * sin(2 * PI * (300 * t + 4_500 * t * t)) }
            }
            // Un legnetto: due note insieme, corte.
            CounterSound.WOOD -> {
                seconds = 0.06
                wave = { t -> 0.7 * exp(-t / 0.012) * (0.7 * sin(2 * PI * 900 * t) + 0.3 * sin(2 * PI * 2300 * t)) }
            }
            // Un bip morbido.
            CounterSound.BEEP -> {
                seconds = 0.08
                wave = { t -> 0.35 * envelope(t, seconds, 0.005) * sin(2 * PI * 880 * t) }
            }
            // Una moneta: due note, la seconda più alta e più lunga.
            CounterSound.COIN -> {
                seconds = 0.18
                wave = { t ->
                    val note = if (t < 0.04) sin(2 * PI * 988 * t) else sin(2 * PI * 1319 * t) * exp(-(t - 0.04) / 0.05)
                    0.35 * envelope(t, seconds, 0.003) * note
                }
            }
        }
        val count = (seconds * COUNTER_SAMPLE_RATE).roundToInt()
        return ShortArray(count) { i ->
            val value = wave(i.toDouble() / COUNTER_SAMPLE_RATE).coerceIn(-1.0, 1.0)
            (value * Short.MAX_VALUE).roundToInt().toShort()
        }
    }

    /** Un fruscio sempre uguale, da [seed]: un valore fra -1 e 1 per ogni istante. */
    private fun noise(seconds: Double, seed: Int = 3): (Double) -> Double {
        val random = Random(seed)
        val values = DoubleArray((seconds * COUNTER_SAMPLE_RATE).roundToInt() + 1) { random.nextDouble(-1.0, 1.0) }
        return { t -> values[min((t * COUNTER_SAMPLE_RATE).roundToInt(), values.lastIndex)] }
    }

    /** Sale e scende in [ramp] secondi ai due capi, per non schioccare. */
    private fun envelope(t: Double, seconds: Double, ramp: Double): Double =
        min(1.0, min(t / ramp, (seconds - t) / ramp)).coerceAtLeast(0.0)

    /** Il punto più forte dell'onda, fra 0 e 1: per i test (il meno è più lieve del più). */
    fun peak(sound: CounterSound): Double = samples(sound).maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toDouble()

    /**
     * Dalla barra del volume (da 0 a 100) al guadagno della traccia (da 0 a
     * 1): al quadrato, perché l'orecchio non sente il volume in linea retta.
     * Dritto, quasi tutta la barra suonerebbe forte uguale e il piano
     * starebbe tutto in fondo.
     */
    fun gain(volume: Int): Float {
        val share = volume.coerceIn(0, 100) / 100f
        return share * share
    }

    // **Subito, senza far aspettare il numero** (dal 27/10/2026, chiesto
    // dall'utente: il + e il − del Counter avevano "quel piccolo delay").
    // Misurato sul telefono: far partire il suono con `AudioTrack` (stop,
    // ricarica, play) teneva fermo il filo principale 65-74 ms a ogni tocco,
    // e il numero cambiava solo dopo. Ora i suoni stanno in un `SoundPool`, il
    // lettore di Android per i suoni brevi: `play` torna subito e il suono
    // parte dal suo filo, con meno attesa. Finché il `SoundPool` non è pronto
    // (appena aperta l'app) suona come prima, ma da un filo a parte.

    private val worker = Executors.newSingleThreadExecutor()
    @Volatile
    private var pool: SoundPool? = null
    /** I suoni pronti nel `SoundPool`, col loro numero. */
    private val ready = ConcurrentHashMap<CounterSound, Int>()
    private val loading = ConcurrentHashMap<Int, CounterSound>()

    /**
     * Prepara i suoni, una volta sola e da un filo a parte: ognuno diventa un
     * piccolo file WAV nella cache dell'app, che il `SoundPool` carica. Si
     * chiama quando il contatore si vede, così al primo tocco sono pronti.
     */
    fun prepare(context: Context) {
        val dir = File(context.applicationContext.cacheDir, "counter_sounds")
        worker.execute {
            if (pool != null) return@execute
            runCatching {
                val sounds = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(sonification())
                    .build()
                sounds.setOnLoadCompleteListener { _, id, status ->
                    val sound = loading.remove(id)
                    if (status == 0 && sound != null) ready[sound] = id
                }
                pool = sounds
                dir.mkdirs()
                CounterSound.entries.forEach { sound ->
                    val bytes = wav(samples(sound))
                    val file = File(dir, "${sound.name}.wav")
                    if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
                    loading[sounds.load(file.path, 1)] = sound
                }
            }
        }
    }

    /**
     * Suona [sound] a [volume] (da 0 a 100, vedi [gain]). A zero, o senza
     * audio, non fa niente. Non aspetta il suono: torna subito.
     */
    fun play(sound: CounterSound, volume: Int = 100) {
        if (volume <= 0) return
        val gain = gain(volume)
        val id = ready[sound]
        val sounds = pool
        if (id != null && sounds != null) {
            sounds.play(id, gain, gain, 1, 0, 1f)
        } else {
            worker.execute { playWithTrack(sound, gain) }
        }
    }

    /** Il file WAV di un suono: l'intestazione di 44 byte e l'onda, 16 bit mono. */
    internal fun wav(samples: ShortArray): ByteArray {
        val data = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16)
            .putShort(1) // PCM
            .putShort(1) // mono
            .putInt(COUNTER_SAMPLE_RATE)
            .putInt(COUNTER_SAMPLE_RATE * 2)
            .putShort(2)
            .putShort(16)
        buffer.put("data".toByteArray()).putInt(data)
        samples.forEach { buffer.putShort(it) }
        return buffer.array()
    }

    /** Come il suono dei tasti: segue il volume di sistema e il silenzioso. */
    private fun sonification(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val tracks = mutableMapOf<CounterSound, AudioTrack>()

    /** Il modo di prima, finché il `SoundPool` non è pronto: se stava ancora suonando, ricomincia. */
    @Synchronized
    private fun playWithTrack(sound: CounterSound, gain: Float) {
        runCatching {
            val track = tracks.getOrPut(sound) { trackFor(samples(sound)) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.setVolume(gain)
            track.reloadStaticData()
            track.play()
        }
    }

    private fun trackFor(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(sonification())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(COUNTER_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }
}
