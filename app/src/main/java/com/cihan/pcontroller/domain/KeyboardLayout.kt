package com.cihan.pcontroller.domain

import com.cihan.pcontroller.bluetooth.KeyCode

/** Windows’ta seçili olmalı — HID tuş gönderir, karakter üretmez. */
enum class KeyboardLayoutId {
    US,
    TR_Q
}

sealed class SoftKey {
    data class Usage(
        val label: String,
        val shiftLabel: String = label.uppercase(),
        val usage: Int,
        val isLetter: Boolean = false,
        /** Sembol katmanında: basınca Shift bit’i de gönder (örn. @ = Shift+2) */
        val forceShift: Boolean = false
    ) : SoftKey()

    data class Modifier(val kind: ModKind) : SoftKey()
    data class Layer(val toSymbols: Boolean) : SoftKey()
    data object Space : SoftKey()
    data object Enter : SoftKey()
    data object Backspace : SoftKey()
    data object Tab : SoftKey()
    data object Esc : SoftKey()
}

enum class ModKind { Shift, Ctrl, Alt, Caps }

/**
 * Telefon tarzı: 3 harf satırı + alt bar.
 * Rakam / ok / Esc yalnızca ?123 katmanında.
 */
object KeyboardLayouts {

    fun title(id: KeyboardLayoutId): String = when (id) {
        KeyboardLayoutId.US -> "English (US)"
        KeyboardLayoutId.TR_Q -> "Türkçe Q"
    }

    fun alphaRows(id: KeyboardLayoutId): List<List<SoftKey>> = when (id) {
        KeyboardLayoutId.US -> usAlpha()
        KeyboardLayoutId.TR_Q -> trQAlpha()
    }

    fun symbolRows(): List<List<SoftKey>> = listOf(
        listOf(
            u("1", KeyCode.DIGIT_1.usage),
            u("2", KeyCode.DIGIT_2.usage),
            u("3", KeyCode.DIGIT_3.usage),
            u("4", KeyCode.DIGIT_4.usage),
            u("5", KeyCode.DIGIT_5.usage),
            u("6", KeyCode.DIGIT_6.usage),
            u("7", KeyCode.DIGIT_7.usage),
            u("8", KeyCode.DIGIT_8.usage),
            u("9", KeyCode.DIGIT_9.usage),
            u("0", KeyCode.DIGIT_0.usage)
        ),
        listOf(
            shifted("@", KeyCode.DIGIT_2.usage),
            shifted("#", KeyCode.DIGIT_3.usage),
            u("-", KeyCode.MINUS.usage),
            shifted("_", KeyCode.MINUS.usage),
            u("/", KeyCode.SLASH.usage),
            shifted("?", KeyCode.SLASH.usage),
            u(",", KeyCode.COMMA.usage),
            u(".", KeyCode.PERIOD.usage),
            u("'", KeyCode.APOSTROPHE.usage),
            SoftKey.Tab
        ),
        listOf(
            SoftKey.Usage("←", "←", KeyCode.LEFT_ARROW.usage),
            SoftKey.Usage("↑", "↑", KeyCode.UP_ARROW.usage),
            SoftKey.Usage("↓", "↓", KeyCode.DOWN_ARROW.usage),
            SoftKey.Usage("→", "→", KeyCode.RIGHT_ARROW.usage),
            SoftKey.Esc,
            SoftKey.Backspace
        )
    )

    private fun usAlpha(): List<List<SoftKey>> = listOf(
        letters("qwertyuiop"),
        letters("asdfghjkl"),
        listOf(SoftKey.Modifier(ModKind.Shift)) + letters("zxcvbnm") + listOf(SoftKey.Backspace)
    )

    private fun trQAlpha(): List<List<SoftKey>> = listOf(
        listOf(
            letter("q", KeyCode.Q.usage),
            letter("w", KeyCode.W.usage),
            letter("e", KeyCode.E.usage),
            letter("r", KeyCode.R.usage),
            letter("t", KeyCode.T.usage),
            letter("y", KeyCode.Y.usage),
            letter("u", KeyCode.U.usage),
            SoftKey.Usage("ı", "I", KeyCode.I.usage, isLetter = true),
            letter("o", KeyCode.O.usage),
            letter("p", KeyCode.P.usage),
            SoftKey.Usage("ğ", "Ğ", KeyCode.LEFT_BRACKET.usage, isLetter = true),
            SoftKey.Usage("ü", "Ü", KeyCode.RIGHT_BRACKET.usage, isLetter = true)
        ),
        listOf(
            letter("a", KeyCode.A.usage),
            letter("s", KeyCode.S.usage),
            letter("d", KeyCode.D.usage),
            letter("f", KeyCode.F.usage),
            letter("g", KeyCode.G.usage),
            letter("h", KeyCode.H.usage),
            letter("j", KeyCode.J.usage),
            letter("k", KeyCode.K.usage),
            letter("l", KeyCode.L.usage),
            SoftKey.Usage("ş", "Ş", KeyCode.SEMICOLON.usage, isLetter = true),
            SoftKey.Usage("i", "İ", KeyCode.APOSTROPHE.usage, isLetter = true)
        ),
        listOf(
            SoftKey.Modifier(ModKind.Shift),
            letter("z", KeyCode.Z.usage),
            letter("x", KeyCode.X.usage),
            letter("c", KeyCode.C.usage),
            letter("v", KeyCode.V.usage),
            letter("b", KeyCode.B.usage),
            letter("n", KeyCode.N.usage),
            letter("m", KeyCode.M.usage),
            SoftKey.Usage("ö", "Ö", KeyCode.COMMA.usage, isLetter = true),
            SoftKey.Usage("ç", "Ç", KeyCode.PERIOD.usage, isLetter = true),
            SoftKey.Backspace
        )
    )

    private fun letters(s: String): List<SoftKey> =
        s.map { letter(it.toString(), letterUsage(it)) }

    private fun letter(ch: String, usage: Int) =
        SoftKey.Usage(ch, ch.uppercase(), usage, isLetter = true)

    private fun u(label: String, usage: Int) =
        SoftKey.Usage(label, label, usage)

    private fun shifted(label: String, usage: Int) =
        SoftKey.Usage(label, label, usage, forceShift = true)

    private fun letterUsage(c: Char): Int = when (c.lowercaseChar()) {
        'a' -> KeyCode.A.usage
        'b' -> KeyCode.B.usage
        'c' -> KeyCode.C.usage
        'd' -> KeyCode.D.usage
        'e' -> KeyCode.E.usage
        'f' -> KeyCode.F.usage
        'g' -> KeyCode.G.usage
        'h' -> KeyCode.H.usage
        'i' -> KeyCode.I.usage
        'j' -> KeyCode.J.usage
        'k' -> KeyCode.K.usage
        'l' -> KeyCode.L.usage
        'm' -> KeyCode.M.usage
        'n' -> KeyCode.N.usage
        'o' -> KeyCode.O.usage
        'p' -> KeyCode.P.usage
        'q' -> KeyCode.Q.usage
        'r' -> KeyCode.R.usage
        's' -> KeyCode.S.usage
        't' -> KeyCode.T.usage
        'u' -> KeyCode.U.usage
        'v' -> KeyCode.V.usage
        'w' -> KeyCode.W.usage
        'x' -> KeyCode.X.usage
        'y' -> KeyCode.Y.usage
        'z' -> KeyCode.Z.usage
        else -> 0
    }
}
