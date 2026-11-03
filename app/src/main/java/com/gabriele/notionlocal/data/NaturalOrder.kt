package com.gabriele.notionlocal.data

/**
 * **L'ordine "naturale" dei testi** (dal 20/10/2026, chiesto dall'utente per
 * Habit Tracker, le cui righe si chiamano coi giorni del mese): i numeri
 * dentro il testo valgono per quello che sono, non lettera per lettera. Così
 * "2" viene prima di "10", e "Giorno 9" prima di "Giorno 10"; prima era 1, 10,
 * 11… 19, 2, 20. Maiuscole e minuscole non contano, come prima.
 *
 * A parità ("01" e "1", "a" e "A") decide il confronto di sempre, così
 * l'ordine non cambia da una volta all'altra.
 */
val NaturalOrder: Comparator<String> = Comparator { a, b ->
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        if (a[i].isDigit() && b[j].isDigit()) {
            val startA = i
            while (i < a.length && a[i].isDigit()) i++
            val startB = j
            while (j < b.length && b[j].isDigit()) j++
            // Il numero più lungo, senza zeri davanti, è il più grande.
            val numberA = a.substring(startA, i).trimStart('0')
            val numberB = b.substring(startB, j).trimStart('0')
            if (numberA.length != numberB.length) return@Comparator numberA.length.compareTo(numberB.length)
            val byDigits = numberA.compareTo(numberB)
            if (byDigits != 0) return@Comparator byDigits
        } else {
            val byLetter = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
            if (byLetter != 0) return@Comparator byLetter
            i++
            j++
        }
    }
    val rest = (a.length - i).compareTo(b.length - j)
    if (rest != 0) return@Comparator rest
    val ignoringCase = String.CASE_INSENSITIVE_ORDER.compare(a, b)
    if (ignoringCase != 0) ignoringCase else a.compareTo(b)
}
