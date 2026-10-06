package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import com.gabriele.notionlocal.data.settings.AppSettings

/**
 * **I linguaggi del blocco "Code"** (dal 06/10/2026): quelle più usate, con i
 * nomi del selettore di Notion (ottava foto dell'utente), in ordine
 * alfabetico. "Plain Text" non colora niente.
 */
internal val CODE_LANGUAGES = listOf(
    "Plain Text", "Arduino", "Assembly", "Bash", "C", "C#", "C++", "Clojure", "CoffeeScript", "CSS",
    "Dart", "Diff", "Docker", "Elixir", "Elm", "Erlang", "F#", "Fortran", "Go", "GraphQL",
    "Groovy", "Haskell", "HTML", "Java", "JavaScript", "JSON", "Julia", "Kotlin", "LaTeX", "Less",
    "Lisp", "Lua", "Makefile", "Markdown", "MATLAB", "Objective-C", "OCaml", "Pascal", "Perl", "PHP",
    "PowerShell", "Prolog", "Python", "R", "Ruby", "Rust", "Sass", "Scala", "Scheme", "SCSS",
    "Shell", "Solidity", "SQL", "Swift", "TOML", "TypeScript", "VB.Net", "Visual Basic", "XML", "YAML"
)

/** Il linguaggio con cui nasce un blocco di codice: lo stesso di Notion. */
internal const val DEFAULT_CODE_LANGUAGE = "JavaScript"

/**
 * Come si scrive un linguaggio, per colorarlo: le parole chiave, i commenti e
 * le virgolette. Non è un compilatore: bastano a far leggere il codice a
 * colpo d'occhio, come fa Notion.
 */
private class Syntax(
    val keywords: Set<String>,
    val lineComments: List<String> = listOf("//"),
    val blockComments: List<Pair<String, String>> = listOf("/*" to "*/"),
    val quotes: String = "\"'`",
    val caseInsensitive: Boolean = false
)

private fun words(text: String): Set<String> = text.split(' ', '\n').filter { it.isNotBlank() }.toSet()

private val C_WORDS = words(
    "if else for while do switch case default break continue return goto struct union enum typedef sizeof " +
        "static const volatile extern void int long short char float double signed unsigned auto register inline " +
        "true false NULL nullptr bool"
)
private val CPP_WORDS = C_WORDS + words(
    "class public private protected virtual override template typename namespace using new delete this " +
        "try catch throw friend operator explicit constexpr noexcept std"
)
private val JAVA_WORDS = words(
    "if else for while do switch case default break continue return class interface extends implements new " +
        "this super public private protected abstract final native synchronized transient throws throw try catch " +
        "finally import package instanceof boolean byte char short int long float double void var record sealed " +
        "permits yield static enum true false null assert"
)
private val KOTLIN_WORDS = words(
    "fun val var class object interface data sealed enum when is in as if else for while do return break " +
        "continue try catch finally throw import package private public protected internal override open abstract " +
        "lateinit by companion suspend null true false this super typealias init get set inline reified operator " +
        "infix const vararg"
)
private val JS_WORDS = words(
    "var let const function return if else for while do switch case default break continue new delete typeof " +
        "instanceof in of class extends super this import export from as async await yield try catch finally " +
        "throw null undefined true false void static get set"
)
private val TS_WORDS = JS_WORDS + words(
    "interface type enum implements namespace declare readonly public private protected abstract any number " +
        "string boolean unknown never keyof"
)
private val PYTHON_WORDS = words(
    "def class return if elif else for while in not and or is import from as with try except finally raise " +
        "pass break continue lambda yield global nonlocal assert del None True False async await self match case"
)
private val CSHARP_WORDS = JAVA_WORDS + words(
    "namespace using struct internal readonly ref out params override virtual async await get set string " +
        "object decimal bool foreach is as base lock"
)
private val GO_WORDS = words(
    "break case chan const continue default defer else fallthrough for func go goto if import interface map " +
        "package range return select struct switch type var true false nil string int bool error"
)
private val RUST_WORDS = words(
    "as async await break const continue crate dyn else enum extern false fn for if impl in let loop match " +
        "mod move mut pub ref return self Self static struct super trait true type unsafe use where while Some " +
        "None Ok Err"
)
private val SWIFT_WORDS = words(
    "class struct enum protocol extension func var let if else guard for while repeat switch case default " +
        "break continue return import public private internal fileprivate open static self Self init deinit " +
        "nil true false throws throw try catch do in as is where async await"
)
private val RUBY_WORDS = words(
    "def end class module if elsif else unless while until for in do return yield begin rescue ensure raise " +
        "nil true false self require attr_accessor puts then case when"
)
private val PHP_WORDS = words(
    "function return if else elseif for foreach while do switch case default break continue class extends " +
        "implements new public private protected static echo print array null true false use namespace try " +
        "catch throw as"
)
private val SQL_WORDS = words(
    "select from where insert into values update set delete create table drop alter add join left right inner " +
        "outer on as and or not null is in like order by group having limit offset distinct union all primary key " +
        "foreign references index view count sum avg min max case when then else end"
)
private val SHELL_WORDS = words(
    "if then else elif fi for in do done while until case esac function return local export echo exit read " +
        "set unset source true false"
)
private val DART_WORDS = JAVA_WORDS + words("final late required dynamic async await Future void")
private val LUA_WORDS = words(
    "and break do else elseif end false for function if in local nil not or repeat return then true until while"
)
private val GENERIC_WORDS = JS_WORDS + PYTHON_WORDS + C_WORDS

private fun syntaxFor(language: String): Syntax? = when (language) {
    "Plain Text" -> null
    "C", "Arduino", "Objective-C" -> Syntax(C_WORDS)
    "C++" -> Syntax(CPP_WORDS)
    "C#", "VB.Net" -> Syntax(CSHARP_WORDS)
    "Java", "Groovy", "Scala" -> Syntax(JAVA_WORDS)
    "Kotlin" -> Syntax(KOTLIN_WORDS)
    "JavaScript", "CoffeeScript", "JSON" -> Syntax(JS_WORDS)
    "TypeScript" -> Syntax(TS_WORDS)
    "Dart" -> Syntax(DART_WORDS)
    "Go" -> Syntax(GO_WORDS)
    "Rust", "Solidity" -> Syntax(RUST_WORDS)
    "Swift" -> Syntax(SWIFT_WORDS)
    "PHP" -> Syntax(PHP_WORDS, lineComments = listOf("//", "#"))
    "Python" -> Syntax(PYTHON_WORDS, lineComments = listOf("#"), blockComments = emptyList())
    "Ruby", "Elixir" -> Syntax(RUBY_WORDS, lineComments = listOf("#"), blockComments = emptyList())
    "Bash", "Shell", "PowerShell", "Makefile", "Docker", "Perl", "R", "Julia" ->
        Syntax(SHELL_WORDS, lineComments = listOf("#"), blockComments = emptyList())
    "YAML", "TOML" -> Syntax(words("true false null yes no"), lineComments = listOf("#"), blockComments = emptyList(), quotes = "\"'")
    "SQL" -> Syntax(SQL_WORDS, lineComments = listOf("--"), caseInsensitive = true, quotes = "\"'")
    "Lua" -> Syntax(LUA_WORDS, lineComments = listOf("--"), blockComments = listOf("--[[" to "]]"))
    "Haskell", "Elm" -> Syntax(words("module import where let in case of if then else data type class instance deriving do"), lineComments = listOf("--"), blockComments = listOf("{-" to "-}"))
    "HTML", "XML", "Markdown" -> Syntax(emptySet(), lineComments = emptyList(), blockComments = listOf("<!--" to "-->"), quotes = "\"'")
    "CSS", "SCSS", "Sass", "Less" -> Syntax(words("important media import from to"), lineComments = listOf("//"), quotes = "\"'")
    "LaTeX", "MATLAB", "Erlang", "Prolog" -> Syntax(words("function end if else elseif for while return"), lineComments = listOf("%"), blockComments = emptyList(), quotes = "\"'")
    "Lisp", "Clojure", "Scheme" -> Syntax(words("defun defn define let lambda if cond else nil t true false"), lineComments = listOf(";"), blockComments = emptyList(), quotes = "\"")
    "Diff" -> null
    else -> Syntax(GENERIC_WORDS, lineComments = listOf("//", "#"))
}

/** I colori del codice: quelli dei temi "One" (scuro e chiaro), che si leggono bene su tutti e due gli sfondi. */
private class CodePalette(val keyword: Color, val string: Color, val comment: Color, val number: Color, val function: Color, val type: Color)

private val DARK_PALETTE = CodePalette(
    keyword = Color(0xFFC678DD), string = Color(0xFF98C379), comment = Color(0xFF7F848E),
    number = Color(0xFFD19A66), function = Color(0xFF61AFEF), type = Color(0xFFE5C07B)
)
private val LIGHT_PALETTE = CodePalette(
    keyword = Color(0xFFA626A4), string = Color(0xFF50A14F), comment = Color(0xFFA0A1A7),
    number = Color(0xFF986801), function = Color(0xFF4078F2), type = Color(0xFFC18401)
)

/**
 * **Il codice colorato** (dal 06/10/2026): parole chiave, stringhe,
 * commenti, numeri, funzioni (un nome seguito da "(") e tipi (un nome che
 * comincia con la maiuscola). Il testo resta quello: cambiano solo i colori.
 */
internal fun highlightCode(code: String, language: String): AnnotatedString {
    val syntax = syntaxFor(language) ?: return AnnotatedString(code)
    val palette = if (AppSettings.isDark) DARK_PALETTE else LIGHT_PALETTE
    return buildAnnotatedString {
        append(code)
        var i = 0
        fun paint(from: Int, to: Int, style: SpanStyle) {
            if (to > from) addStyle(style, from, to)
        }
        while (i < code.length) {
            val block = syntax.blockComments.firstOrNull { (open, _) -> code.startsWith(open, i) }
            if (block != null) {
                val end = code.indexOf(block.second, i + block.first.length).let {
                    if (it < 0) code.length else it + block.second.length
                }
                paint(i, end, SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic))
                i = end
                continue
            }
            if (syntax.lineComments.any { code.startsWith(it, i) }) {
                val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                paint(i, end, SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic))
                i = end
                continue
            }
            val c = code[i]
            if (c in syntax.quotes) {
                var end = i + 1
                while (end < code.length && code[end] != c) {
                    // Le virgolette semplici e doppie finiscono a fine riga; quelle all'indietro no.
                    if (code[end] == '\n' && c != '`') break
                    end += if (code[end] == '\\') 2 else 1
                }
                end = (end + 1).coerceAtMost(code.length)
                paint(i, end, SpanStyle(color = palette.string))
                i = end
                continue
            }
            if (c.isDigit() && (i == 0 || !code[i - 1].isLetterOrDigit() && code[i - 1] != '_')) {
                var end = i + 1
                while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '.' || code[end] == '_')) end++
                paint(i, end, SpanStyle(color = palette.number))
                i = end
                continue
            }
            if (c.isLetter() || c == '_' || c == '$' || c == '@') {
                var end = i + 1
                while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '_' || code[end] == '$')) end++
                val word = code.substring(i, end)
                val isKeyword = if (syntax.caseInsensitive) word.lowercase() in syntax.keywords else word in syntax.keywords
                var next = end
                while (next < code.length && code[next] == ' ') next++
                when {
                    isKeyword -> paint(i, end, SpanStyle(color = palette.keyword))
                    next < code.length && code[next] == '(' -> paint(i, end, SpanStyle(color = palette.function))
                    word.first().isUpperCase() && word.length > 1 -> paint(i, end, SpanStyle(color = palette.type))
                }
                i = end
                continue
            }
            i++
        }
    }
}
