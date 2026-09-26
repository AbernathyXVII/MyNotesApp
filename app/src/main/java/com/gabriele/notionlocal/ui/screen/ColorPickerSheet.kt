package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Il selettore di colore del pennello: lo stesso di Windows, che è
 * quello che l'utente ha chiesto guardando.
 *
 * Tre modi di scegliere lo stesso colore, e sono **sempre d'accordo fra
 * loro** perché uno solo è la verità: la terna tinta/saturazione/
 * luminosità. Il riquadro grande muove tinta (da sinistra a destra) e
 * saturazione (dall'alto in basso); la striscia a destra la luminosità,
 * dal bianco in cima al nero in fondo; i riquadri Rosso/Verde/Blu ed
 * Hex scrivono e leggono la stessa terna. Toccando un punto qualsiasi
 * si aggiorna tutto il resto.
 *
 * Perché HSL e non RGB come stato: muovendo un pallino in un riquadro
 * di colori si ragiona per "che tinta" e "quanto accesa", e tenere RGB
 * come verità vorrebbe dire ricalcolare la posizione del pallino a ogni
 * giro — con i grigi, dove la tinta non esiste più, il pallino
 * schizzerebbe a sinistra da solo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    /** Il colore da cui partire, o null per il colore di sempre. */
    initialHex: String?,
    /** false = colore del testo, true = colore dietro al testo. */
    background: Boolean,
    onDismiss: () -> Unit,
    onSwitchTarget: (background: Boolean) -> Unit,
    onPick: (String?) -> Unit,
    /**
     * I due riquadri "Testo" e "Sfondo" in cima. Spenti per chi colora
     * una cosa sola, come le barre dell'avanzamento nei widget: lì
     * scegliere "dietro al testo" non vorrebbe dire niente.
     */
    showTargetSwitch: Boolean = true
) {
    // **Lo stato si crea una volta sola, all'apertura.**
    //
    // Prima era legato anche a `background`, così passando da "Text" a
    // "Background" ripartiva dal colore iniziale. Sembrava ragionevole e
    // invece rompeva tutto: `remember` con una chiave nuova fabbrica uno
    // stato **nuovo**, e i gestori dei tocchi — che una `pointerInput`
    // si tiene congelati — continuavano a scrivere in quello vecchio,
    // ormai buttato via. Da fuori: il pallino non si muoveva più.
    //
    // Cambiando bersaglio il colore adesso resta quello che è, il che è
    // anche più comodo: quasi sempre si vuole lo stesso colore, o una
    // sua correzione, non ricominciare da capo.
    var hsl by remember { mutableStateOf(hexToHsl(initialHex) ?: Hsl(0f, 1f, 0.5f)) }
    // Quello che c'è scritto nel riquadro Hex mentre lo si sta
    // scrivendo: non si può tenere il colore come unica verità, perché
    // a metà di "#FF00" non c'è ancora nessun colore da mostrare.
    var hexText by remember { mutableStateOf(hsl.toHex()) }

    fun setHsl(next: Hsl) {
        hsl = next
        hexText = next.toHex()
    }

    val color = hsl.toColor()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Testo o sfondo: due riquadri che si accendono, invece di
            // due finestre separate. Cambiare bersaglio senza chiudere
            // è la cosa che si fa di continuo — si sceglie un colore,
            // si guarda, si prova a metterlo dietro invece che davanti.
            if (showTargetSwitch) Row(modifier = Modifier.fillMaxWidth()) {
                TargetChip(
                    label = EditorStrings.colorText,
                    selected = !background,
                    onClick = { onSwitchTarget(false) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(8.dp))
                TargetChip(
                    label = EditorStrings.colorBackground,
                    selected = background,
                    onClick = { onSwitchTarget(true) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                HueSaturationField(
                    hsl = hsl,
                    onChange = { hue, saturation ->
                        setHsl(hsl.copy(hue = hue, saturation = saturation))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                LightnessStrip(
                    hsl = hsl,
                    onChange = { setHsl(hsl.copy(lightness = it)) },
                    modifier = Modifier
                        .width(28.dp)
                        .height(180.dp)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            // L'anteprima è scritta sopra e dietro: un quadratino da
            // solo non direbbe come viene a leggersi davvero, che è la
            // sola cosa che interessa quando si colora del testo.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (background) color else DarkSurface)
                        .border(1.dp, NotionGray400, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aa",
                        color = if (background) NotionWhite else color,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = if (background) EditorStrings.backgroundColor else EditorStrings.textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = hsl.toHex(),
                        style = MaterialTheme.typography.labelMedium,
                        color = NotionGray400
                    )
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            val rgb = hsl.toRgb()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChannelField(
                    label = EditorStrings.red,
                    value = rgb.first,
                    onValue = { setHsl(rgbToHsl(it, rgb.second, rgb.third)) },
                    modifier = Modifier.weight(1f)
                )
                ChannelField(
                    label = EditorStrings.green,
                    value = rgb.second,
                    onValue = { setHsl(rgbToHsl(rgb.first, it, rgb.third)) },
                    modifier = Modifier.weight(1f)
                )
                ChannelField(
                    label = EditorStrings.blue,
                    value = rgb.third,
                    onValue = { setHsl(rgbToHsl(rgb.first, rgb.second, it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column {
                Text("Hex", style = MaterialTheme.typography.labelMedium, color = NotionGray400)
                Spacer(modifier = Modifier.size(4.dp))
                BasicTextField(
                    value = hexText,
                    onValueChange = { typed ->
                        // Si scrive liberamente — il codice arriva da
                        // fuori, incollato o copiato a mano — e il
                        // colore si muove solo quando quello che c'è
                        // scritto è un colore vero. Così a metà di
                        // "#FF00" non salta niente.
                        hexText = typed.take(7)
                        hexToHsl(typed)?.let { hsl = it }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                    cursorBrush = SolidColor(NotionWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Togliere il colore è una cosa che si fa spesso quanto
                // metterlo, e da qualche parte doveva stare.
                TextButton(onClick = { onPick(null) }) { Text(EditorStrings.noColor) }
                Spacer(modifier = Modifier.size(8.dp))
                TextButton(onClick = { onPick(hsl.toHex()) }) { Text(EditorStrings.apply) }
            }

            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun TargetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else DarkSurface)
            .pointerInput(selected) { detectTapGestures { onClick() } }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else NotionWhite,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Il riquadro grande: tinta da sinistra a destra, saturazione
 * dall'alto in basso.
 *
 * Disegnato con due sfumature sovrapposte invece che pixel per pixel —
 * l'arcobaleno orizzontale, e sopra un bianco che si fa via via più
 * coprente scendendo. Calcolando ogni punto a mano il riquadro
 * diventerebbe migliaia di rettangolini da ridisegnare a ogni
 * trascinamento del dito.
 */
@Composable
private fun HueSaturationField(
    hsl: Hsl,
    onChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // La misura arriva da `onSizeChanged` e non dal disegno: scriverla
    // mentre si disegna rimetterebbe in moto il disegno, all'infinito.
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // **`pointerInput` si tiene la lambda che ha ricevuto la prima
    // volta.** Con una chiave fissa non la rinfresca più, e un gestore
    // vecchio finisce per scrivere dove non guarda più nessuno. Tenuto
    // aggiornato qui, il tocco chiama sempre quello di adesso. Il
    // riquadro manda solo **tinta e saturazione**, non un colore
    // intero: così non ha bisogno di conoscere il resto, e non può
    // riportare indietro una luminosità vecchia.
    val currentOnChange by rememberUpdatedState(onChange)

    fun pick(position: Offset) {
        if (boxSize.width <= 0 || boxSize.height <= 0) return
        val hue = (position.x / boxSize.width).coerceIn(0f, 1f) * 360f
        val saturation = 1f - (position.y / boxSize.height).coerceIn(0f, 1f)
        currentOnChange(hue, saturation)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) { detectTapGestures { pick(it) } }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    pick(change.position)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    (0..6).map { Color.hsl(it * 60f % 360f, 1f, 0.5f) }
                )
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.White))
            )
            // Il pallino: due cerchi, bianco dentro e nero fuori, così
            // si vede sopra qualunque colore ci finisca sotto. Tenuto
            // **dentro i bordi**: sul rosso pieno cadrebbe nell'angolo
            // in alto a sinistra e si vedrebbe per un quarto, cioè
            // sembrerebbe non esserci.
            val margin = 14f
            val center = Offset(
                ((hsl.hue / 360f) * size.width).coerceIn(margin, size.width - margin),
                ((1f - hsl.saturation) * size.height).coerceIn(margin, size.height - margin)
            )
            drawCircle(Color.White, radius = 9f, center = center, style = markerStroke())
            drawCircle(Color.Black, radius = 12f, center = center, style = markerStroke())
        }
    }
}

/** La striscia a destra: bianco in cima, il colore a metà, nero in fondo. */
@Composable
private fun LightnessStrip(
    hsl: Hsl,
    onChange: (lightness: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    // Vedi il gemello nel riquadro grande: la lambda va tenuta fresca.
    val currentOnChange by rememberUpdatedState(onChange)

    fun pick(position: Offset) {
        if (boxSize.height <= 0) return
        currentOnChange(1f - (position.y / boxSize.height).coerceIn(0f, 1f))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) { detectTapGestures { pick(it) } }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    pick(change.position)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White,
                        Color.hsl(hsl.hue, hsl.saturation.coerceAtLeast(0.0001f), 0.5f),
                        Color.Black
                    )
                )
            )
            val y = (1f - hsl.lightness) * size.height
            drawLine(
                color = Color.White,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Black,
                start = Offset(0f, y + 3f),
                end = Offset(size.width, y + 3f),
                strokeWidth = 1.5f
            )
        }
    }
}

private fun markerStroke() = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)

@Composable
private fun ChannelField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = NotionGray400)
        Spacer(modifier = Modifier.size(4.dp))
        BasicTextField(
            value = value.toString(),
            onValueChange = { typed ->
                // Solo cifre, e mai oltre 255: un canale non ha altri
                // valori possibili, e lasciarne scrivere di sbagliati
                // vorrebbe dire doverli poi rifiutare.
                val cleaned = typed.filter { it.isDigit() }.take(3)
                onValue(cleaned.toIntOrNull()?.coerceIn(0, 255) ?: 0)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
            cursorBrush = SolidColor(NotionWhite),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        )
    }
}

// --- Tinta, saturazione, luminosità ---

/** Tinta in gradi (0..360), saturazione e luminosità in frazioni (0..1). */
data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)

fun Hsl.toColor(): Color = Color.hsl(
    hue.coerceIn(0f, 360f),
    saturation.coerceIn(0f, 1f),
    lightness.coerceIn(0f, 1f)
)

fun Hsl.toRgb(): Triple<Int, Int, Int> {
    val c = toColor()
    return Triple(
        (c.red * 255f).roundToInt(),
        (c.green * 255f).roundToInt(),
        (c.blue * 255f).roundToInt()
    )
}

fun Hsl.toHex(): String {
    val (r, g, b) = toRgb()
    return "#%02X%02X%02X".format(r, g, b)
}

/**
 * Da `#RRGGBB` (o `RRGGBB`) alla terna, o null se quello che c'è
 * scritto non è ancora un colore: è il caso normale mentre lo si
 * scrive a mano.
 */
fun hexToHsl(hex: String?): Hsl? {
    val cleaned = hex?.trim()?.removePrefix("#") ?: return null
    if (cleaned.length != 6 || !cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    return rgbToHsl(
        ((value shr 16) and 0xFF).toInt(),
        ((value shr 8) and 0xFF).toInt(),
        (value and 0xFF).toInt()
    )
}

/** Il colore Compose di un esadecimale, o null se non è un colore. */
fun hexToColor(hex: String?): Color? = hexToHsl(hex)?.toColor()

fun rgbToHsl(red: Int, green: Int, blue: Int): Hsl {
    val r = red.coerceIn(0, 255) / 255f
    val g = green.coerceIn(0, 255) / 255f
    val b = blue.coerceIn(0, 255) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val lightness = (max + min) / 2f

    // Grigio: la tinta non esiste. Restituirne una qualsiasi farebbe
    // saltare il pallino a sinistra ogni volta che si passa per un
    // grigio, quindi si tiene zero e basta.
    if (abs(delta) < 1e-6f) return Hsl(0f, 0f, lightness)

    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return Hsl(if (hue < 0f) hue + 360f else hue, saturation.coerceIn(0f, 1f), lightness)
}
