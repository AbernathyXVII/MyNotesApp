package com.gabriele.notionlocal.data.formula

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **Le formule dei database** (dal 28/10/2026, chieste dall'utente: "la
 * proprietà Formula di Notion: giorni che mancano, prezzo × quantità, se è
 * spuntato allora…"): un valore calcolato riga per riga dalle altre proprietà.
 *
 * Il linguaggio è quello di Notion: `prop("Prezzo") * prop("Quantità")`,
 * `if(prop("Fatto"), "Finito", "Da fare")`, `dateBetween(prop("Scadenza"),
 * now(), "days")`. Qui come si legge una formula (`FormulaParser`); come si
 * calcola sta in `FormulaEngine`.
 */
@Serializable
data class FormulaConfig(val expression: String = "") {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        /** La formula salvata in una colonna; quella di un altro tipo ("[]") vale come vuota. */
        fun parse(text: String?): FormulaConfig =
            text?.takeIf { it.trimStart().startsWith("{") }
                ?.let { runCatching { json.decodeFromString<FormulaConfig>(it) }.getOrNull() }
                ?: FormulaConfig()

        fun encode(config: FormulaConfig): String = json.encodeToString(config)
    }
}

/** Il risultato di una formula per una riga. */
sealed class FormulaValue {
    /** Niente: una proprietà vuota, o una formula vuota. */
    data object Empty : FormulaValue()
    data class Num(val value: Double) : FormulaValue()
    data class Text(val value: String) : FormulaValue()
    data class Bool(val value: Boolean) : FormulaValue()

    /** Una data: l'inizio, se conta l'ora, e la fine se è un intervallo. */
    data class Date(val millis: Long, val hasTime: Boolean = false, val end: Long? = null) : FormulaValue()
    data class Error(val error: FormulaError) : FormulaValue()
}

/** Cosa c'è che non va in una formula: il tipo e il pezzo che lo dice (il nome, la funzione). */
data class FormulaError(val kind: Kind, val detail: String = "") {
    enum class Kind {
        /** Scritta male, senza un motivo più preciso. */
        SYNTAX,
        /** Un pezzo che lì non ci va: [detail] è il pezzo. */
        UNEXPECTED,
        /** Manca un segno: [detail] è il segno (")", ":", le virgolette). */
        MISSING,
        /** La formula finisce a metà ("2 +"). */
        INCOMPLETE,
        /** Un nome senza parentesi e senza virgolette: [detail] è il nome. */
        UNKNOWN_NAME,
        /** prop() senza il nome fra virgolette. */
        PROP_NEEDS_NAME,
        /** `prop("…")` di una proprietà che non c'è. */
        UNKNOWN_PROPERTY,
        /** Una funzione che non esiste. */
        UNKNOWN_FUNCTION,
        /** Una funzione con troppi o troppo pochi valori. */
        WRONG_ARGUMENTS,
        /** Un'operazione fra valori che non vanno insieme (un testo diviso un numero). */
        TYPE,
        /** Due formule che si chiamano a vicenda. */
        CIRCULAR,
        DIVISION_BY_ZERO
    }
}

class FormulaException(val error: FormulaError) : RuntimeException(error.kind.name + ": " + error.detail)

/** Una formula letta: l'albero delle operazioni. */
sealed class Expr {
    data class Num(val value: Double) : Expr()
    data class Str(val value: String) : Expr()
    data class Bool(val value: Boolean) : Expr()
    data class Prop(val name: String) : Expr()
    data class Unary(val op: String, val operand: Expr) : Expr()
    data class Binary(val op: String, val left: Expr, val right: Expr) : Expr()
    data class Call(val name: String, val args: List<Expr>) : Expr()
    data class Cond(val condition: Expr, val then: Expr, val otherwise: Expr) : Expr()
}

private data class Token(val kind: Kind, val text: String, val pos: Int) {
    enum class Kind { NUMBER, STRING, IDENT, OP, LPAREN, RPAREN, COMMA, QUESTION, COLON, END }
}

/**
 * **Legge una formula.** Le priorità, dalla più bassa: `? :`, `or` (`||`),
 * `and` (`&&`), `not` (`!`), `==` `!=`, `<` `<=` `>` `>=`, `+` `-`, `*` `/` `%`,
 * `^` (da destra), il meno davanti, le funzioni e le parentesi. Una formula
 * vuota non è un errore: vale niente.
 */
object FormulaParser {

    fun parse(text: String): Expr? {
        val tokens = tokenize(text)
        if (tokens.size == 1) return null
        val parser = Reader(tokens)
        val expr = parser.ternary()
        if (parser.peek().kind != Token.Kind.END) parser.fail(FormulaError.Kind.UNEXPECTED, parser.peek().text)
        return expr
    }

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || (c == '.' && i + 1 < text.length && text[i + 1].isDigit()) -> {
                    val start = i
                    while (i < text.length && (text[i].isDigit() || text[i] == '.')) i++
                    // 1e3, 2.5E-2
                    if (i < text.length && (text[i] == 'e' || text[i] == 'E') &&
                        i + 1 < text.length && (text[i + 1].isDigit() || ((text[i + 1] == '-' || text[i + 1] == '+') && i + 2 < text.length && text[i + 2].isDigit()))
                    ) {
                        i += 2
                        while (i < text.length && text[i].isDigit()) i++
                    }
                    tokens += Token(Token.Kind.NUMBER, text.substring(start, i), start)
                }
                c == '"' || c == '“' || c == '”' || c == '\'' -> {
                    // Le virgolette "curve" della tastiera valgono come quelle dritte.
                    val start = i
                    val closing = if (c == '\'') setOf('\'') else setOf('"', '”', '“')
                    i++
                    val sb = StringBuilder()
                    while (i < text.length && text[i] !in closing) {
                        if (text[i] == '\\' && i + 1 < text.length) {
                            val next = text[i + 1]
                            sb.append(if (next == 'n') '\n' else next)
                            i += 2
                        } else {
                            sb.append(text[i++])
                        }
                    }
                    if (i >= text.length) throw FormulaException(FormulaError(FormulaError.Kind.MISSING, if (c == '\'') "'" else "\""))
                    i++
                    tokens += Token(Token.Kind.STRING, sb.toString(), start)
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    tokens += Token(Token.Kind.IDENT, text.substring(start, i), start)
                }
                c == '(' -> tokens += Token(Token.Kind.LPAREN, "(", i++)
                c == ')' -> tokens += Token(Token.Kind.RPAREN, ")", i++)
                c == ',' -> tokens += Token(Token.Kind.COMMA, ",", i++)
                c == '?' -> tokens += Token(Token.Kind.QUESTION, "?", i++)
                c == ':' -> tokens += Token(Token.Kind.COLON, ":", i++)
                else -> {
                    val two = if (i + 1 < text.length) text.substring(i, i + 2) else ""
                    val op = when {
                        two in setOf("==", "!=", "<=", ">=", "&&", "||") -> two
                        // Le varianti della tastiera: × ÷ ≤ ≥ ≠
                        c == '×' -> "*"
                        c == '÷' -> "/"
                        c == '≤' -> "<="
                        c == '≥' -> ">="
                        c == '≠' -> "!="
                        c in "+-*/%^<>!=" -> c.toString()
                        else -> throw FormulaException(FormulaError(FormulaError.Kind.UNEXPECTED, c.toString()))
                    }
                    tokens += Token(Token.Kind.OP, if (op == "=") "==" else op, i)
                    i += if (two == op) 2 else 1
                }
            }
        }
        tokens += Token(Token.Kind.END, "", text.length)
        return tokens
    }

    private class Reader(private val tokens: List<Token>) {
        private var at = 0

        fun peek(): Token = tokens[at]
        private fun next(): Token = tokens[at++]

        fun fail(kind: FormulaError.Kind, detail: String = ""): Nothing = throw FormulaException(FormulaError(kind, detail))

        private fun isOp(vararg ops: String): Boolean {
            val t = peek()
            return (t.kind == Token.Kind.OP && t.text in ops) ||
                (t.kind == Token.Kind.IDENT && t.text.lowercase() in ops)
        }

        private fun expect(kind: Token.Kind, what: String) {
            if (peek().kind != kind) {
                if (peek().kind == Token.Kind.END) fail(FormulaError.Kind.MISSING, what) else fail(FormulaError.Kind.UNEXPECTED, peek().text)
            }
            next()
        }

        fun ternary(): Expr {
            val condition = or()
            if (peek().kind != Token.Kind.QUESTION) return condition
            next()
            val then = ternary()
            expect(Token.Kind.COLON, ":")
            val otherwise = ternary()
            return Expr.Cond(condition, then, otherwise)
        }

        private fun or(): Expr {
            var left = and()
            while (isOp("||", "or")) {
                next()
                left = Expr.Binary("or", left, and())
            }
            return left
        }

        private fun and(): Expr {
            var left = not()
            while (isOp("&&", "and")) {
                next()
                left = Expr.Binary("and", left, not())
            }
            return left
        }

        private fun not(): Expr {
            // "not" come funzione, not(x), la legge `primary`.
            if (isOp("!") || (isOp("not") && tokens[at + 1].kind != Token.Kind.LPAREN)) {
                next()
                return Expr.Unary("not", not())
            }
            return equality()
        }

        private fun equality(): Expr {
            var left = comparison()
            while (isOp("==", "!=")) {
                val op = next().text
                left = Expr.Binary(op, left, comparison())
            }
            return left
        }

        private fun comparison(): Expr {
            var left = additive()
            while (isOp("<", "<=", ">", ">=")) {
                val op = next().text
                left = Expr.Binary(op, left, additive())
            }
            return left
        }

        private fun additive(): Expr {
            var left = multiplicative()
            while (isOp("+", "-")) {
                val op = next().text
                left = Expr.Binary(op, left, multiplicative())
            }
            return left
        }

        private fun multiplicative(): Expr {
            var left = unary()
            while (isOp("*", "/", "%")) {
                val op = next().text
                left = Expr.Binary(op, left, unary())
            }
            return left
        }

        private fun unary(): Expr {
            if (isOp("-")) {
                next()
                return Expr.Unary("-", unary())
            }
            if (isOp("+")) {
                next()
                return unary()
            }
            return power()
        }

        private fun power(): Expr {
            val base = primary()
            if (isOp("^")) {
                next()
                return Expr.Binary("^", base, unary())
            }
            return base
        }

        private fun primary(): Expr {
            val t = next()
            return when (t.kind) {
                Token.Kind.NUMBER -> Expr.Num(t.text.toDoubleOrNull() ?: fail(FormulaError.Kind.UNEXPECTED, t.text))
                Token.Kind.STRING -> Expr.Str(t.text)
                Token.Kind.LPAREN -> {
                    val inside = ternary()
                    expect(Token.Kind.RPAREN, ")")
                    inside
                }
                Token.Kind.IDENT -> when {
                    peek().kind == Token.Kind.LPAREN -> call(t.text)
                    t.text.equals("true", ignoreCase = true) -> Expr.Bool(true)
                    t.text.equals("false", ignoreCase = true) -> Expr.Bool(false)
                    else -> fail(FormulaError.Kind.UNKNOWN_NAME, t.text)
                }
                Token.Kind.END -> fail(FormulaError.Kind.INCOMPLETE)
                else -> fail(FormulaError.Kind.UNEXPECTED, t.text)
            }
        }

        private fun call(name: String): Expr {
            expect(Token.Kind.LPAREN, "(")
            val args = mutableListOf<Expr>()
            if (peek().kind != Token.Kind.RPAREN) {
                args += ternary()
                while (peek().kind == Token.Kind.COMMA) {
                    next()
                    args += ternary()
                }
            }
            expect(Token.Kind.RPAREN, ")")
            // prop("Nome"): il nome della proprietà, scritto fra virgolette.
            if (name.equals("prop", ignoreCase = true)) {
                val only = args.singleOrNull() as? Expr.Str ?: fail(FormulaError.Kind.PROP_NEEDS_NAME)
                return Expr.Prop(only.value)
            }
            return Expr.Call(name, args)
        }
    }
}
