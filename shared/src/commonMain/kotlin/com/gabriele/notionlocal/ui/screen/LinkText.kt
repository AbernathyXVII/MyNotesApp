package com.gabriele.notionlocal.ui.screen


/** Niente link che eseguono codice, né vuoti. */
fun usableLink(url: String): Boolean = url.isNotBlank() && !url.trim().startsWith("javascript:", ignoreCase = true)

fun decodeEntities(text: String): String =
    Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(text) { m ->
        val code = m.groupValues[1]
        when {
            code.startsWith("#x") || code.startsWith("#X") -> code.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
            code.startsWith("#") -> code.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
            else -> when (code) {
                "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; "nbsp" -> "\u00A0"
                else -> null
            }
        } ?: m.value
    }
