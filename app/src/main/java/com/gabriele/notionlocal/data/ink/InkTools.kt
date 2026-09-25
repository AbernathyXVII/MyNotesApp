package com.gabriele.notionlocal.data.ink

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Come cancella la gomma, come su OneNote. */
enum class EraserMode {
    /** Tutto il tratto toccato. */
    STROKE,

    /** Solo quello che sta sotto la gomma: il tratto si spezza. */
    POINT
}

/** Una delle penne della barra: cosa è, di che colore, quanto larga (dp). */
@Serializable
data class InkPen(val tool: InkTool, val color: Long, val width: Float)

/**
 * **Gli strumenti del disegno**, come li si è lasciati: le penne della
 * barra (ognuna col suo tipo, colore e spessore), quale è in mano, la
 * gomma, e se il dito disegna.
 *
 * `selected`: -1 la gomma, altrimenti l'indice della penna.
 */
@Serializable
data class InkSettings(
    val pens: List<InkPen> = DEFAULT_PENS,
    val selected: Int = 0,
    val eraserMode: EraserMode = EraserMode.STROKE,
    /** Il raggio della gomma, in dp. */
    val eraserRadius: Float = ERASER_SIZES[1],
    /**
     * Se il dito disegna. Si spegne da solo la prima volta che si usa la
     * S Pen: da lì il dito scorre la pagina, e il palmo appoggiato sullo
     * schermo non lascia segni.
     */
    val fingerDraws: Boolean = true
) {
    val eraserSelected: Boolean get() = selected < 0
    val pen: InkPen? get() = pens.getOrNull(selected)
}

/** Le penne di partenza, come nella foto di OneNote dell'utente: penne, matita, pennarello, evidenziatore. */
val DEFAULT_PENS = listOf(
    InkPen(InkTool.PEN, InkStrokeEntity.AUTO_COLOR, 2f),
    InkPen(InkTool.PEN, 0xFF2383E2, 2f),
    InkPen(InkTool.PEN, 0xFFE03E3E, 2f),
    InkPen(InkTool.PENCIL, 0xFF9B9A97, 1.5f),
    InkPen(InkTool.MARKER, 0xFF2E9E44, 5f),
    InkPen(InkTool.HIGHLIGHTER, 0xFFFFE600, 18f)
)

/** I cinque spessori della fila "Thickness", in dp, per ogni tipo di penna. */
fun thicknessPresets(tool: InkTool): List<Float> = when (tool) {
    InkTool.PEN -> listOf(1f, 2f, 3f, 5f, 8f)
    InkTool.PENCIL -> listOf(1f, 1.5f, 2.5f, 4f, 6f)
    InkTool.MARKER -> listOf(3f, 5f, 8f, 12f, 18f)
    InkTool.HIGHLIGHTER -> listOf(8f, 12f, 18f, 26f, 36f)
}

/** I cinque raggi della gomma, in dp. */
val ERASER_SIZES = listOf(4f, 8f, 14f, 22f, 34f)

const val MIN_INK_WIDTH = 0.5f
const val MAX_INK_WIDTH = 60f

/** "−" e "+" accanto ai cinque spessori: un passo più fine, un quarto alla volta. */
fun thinner(width: Float): Float = (width / 1.25f).coerceAtLeast(MIN_INK_WIDTH)
fun thicker(width: Float): Float = (width * 1.25f).coerceAtMost(MAX_INK_WIDTH)

/** Il più vicino dei cinque spessori: è quello che si vede cerchiato. */
fun nearestPreset(presets: List<Float>, width: Float): Int =
    presets.indices.minBy { kotlin.math.abs(presets[it] - width) }

/**
 * **Dove si ricordano gli strumenti**: `SharedPreferences`, come le
 * impostazioni dell'app (vedi `AppSettings`), e in uno stato di Compose,
 * così la barra si ridisegna appena cambiano. `init` all'avvio.
 */
object InkPrefs {

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private val state = mutableStateOf(InkSettings())

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("ink", Context.MODE_PRIVATE)
        state.value = prefs.getString(KEY, null)
            ?.let { runCatching { json.decodeFromString<InkSettings>(it) }.getOrNull() }
            ?.takeIf { it.pens.isNotEmpty() }
            ?: InkSettings()
    }

    val settings: InkSettings get() = state.value

    fun update(change: (InkSettings) -> InkSettings) {
        val next = change(state.value)
        if (next == state.value) return
        state.value = next
        if (::prefs.isInitialized) prefs.edit().putString(KEY, json.encodeToString(next)).apply()
    }

    fun updatePen(index: Int, change: (InkPen) -> InkPen) = update { s ->
        if (index !in s.pens.indices) s else s.copy(pens = s.pens.toMutableList().also { it[index] = change(it[index]) })
    }

    private const val KEY = "settings"
}
