package com.cihan.pccontroller.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.cihan.pccontroller.R
import com.cihan.pccontroller.bluetooth.HidReports
import com.cihan.pccontroller.bluetooth.KeyCode
import com.cihan.pccontroller.domain.KeyboardLayoutId
import com.cihan.pccontroller.domain.KeyboardLayouts
import com.cihan.pccontroller.domain.ModKind
import com.cihan.pccontroller.domain.SoftKey

/**
 * Telefon tarzı HID klavye — Gboard benzeri satırlar, Caps yok, oklar 123’te.
 */
class KeyboardPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    fun interface Listener {
        fun onKey(usage: Int, modifiers: Int)
    }

    var listener: Listener? = null

    private var layoutId: KeyboardLayoutId = KeyboardLayoutId.TR_Q
    private var symbolsLayer = false
    private var shiftOn = false
    private var capsOn = false
    private var ctrlOn = false
    private var altOn = false
    private var lastShiftTapMs = 0L

    private val keyHeight: Int
    private val keyGap: Int

    init {
        orientation = VERTICAL
        gravity = Gravity.BOTTOM
        keyHeight = dp(46)
        keyGap = dp(4)
        rebuild()
    }

    fun setLayout(id: KeyboardLayoutId) {
        if (layoutId == id) return
        layoutId = id
        rebuild()
    }

    fun currentLayout(): KeyboardLayoutId = layoutId

    private fun rebuild() {
        removeAllViews()
        val rows = if (symbolsLayer) {
            KeyboardLayouts.symbolRows()
        } else {
            KeyboardLayouts.alphaRows(layoutId)
        }
        rows.forEach { addRow(it) }
        addRow(bottomRow())
    }

    /** [123] [Ctrl] [Alt] [ Space ] [.] [Enter] */
    private fun bottomRow(): List<SoftKey> = listOf(
        SoftKey.Layer(toSymbols = !symbolsLayer),
        SoftKey.Modifier(ModKind.Ctrl),
        SoftKey.Modifier(ModKind.Alt),
        SoftKey.Space,
        SoftKey.Usage(".", ".", KeyCode.PERIOD.usage),
        SoftKey.Enter
    )

    private fun addRow(keys: List<SoftKey>) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = keyGap
            }
        }
        keys.forEach { key ->
            val weight = when (key) {
                SoftKey.Space -> 4f
                SoftKey.Enter -> 1.5f
                SoftKey.Backspace -> 1.5f
                is SoftKey.Layer -> 1.4f
                is SoftKey.Modifier -> when (key.kind) {
                    ModKind.Shift -> 1.5f
                    else -> 1.2f
                }
                else -> 1f
            }
            row.addView(makeButton(key), LayoutParams(0, keyHeight, weight).apply {
                marginStart = keyGap / 2
                marginEnd = keyGap / 2
            })
        }
        addView(row)
    }

    private fun makeButton(key: SoftKey): Button {
        val active = isModifierActive(key)
        val btn = Button(context, null, 0, R.style.RemoteActionKey).apply {
            minimumWidth = 0
            minimumHeight = 0
            setPadding(dp(2), 0, dp(2), 0)
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                when (key) {
                    is SoftKey.Modifier -> 12f
                    SoftKey.Enter, SoftKey.Backspace, is SoftKey.Layer -> 14f
                    else -> 16f
                }
            )
            isAllCaps = false
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            text = labelFor(key)
            setBackgroundResource(
                if (active) R.drawable.bg_remote_key_accent else R.drawable.bg_remote_key
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (active) R.color.on_accent else R.color.text_primary
                )
            )
        }
        btn.setOnClickListener { onSoftKey(key) }
        return btn
    }

    private fun isModifierActive(key: SoftKey): Boolean = when (key) {
        is SoftKey.Modifier -> when (key.kind) {
            ModKind.Shift -> shiftOn || capsOn
            ModKind.Ctrl -> ctrlOn
            ModKind.Alt -> altOn
            ModKind.Caps -> capsOn
        }
        else -> false
    }

    private fun labelFor(key: SoftKey): String = when (key) {
        is SoftKey.Usage -> {
            val upper = shiftOn || (capsOn && key.isLetter)
            if (upper) key.shiftLabel else key.label
        }
        is SoftKey.Modifier -> when (key.kind) {
            ModKind.Shift -> if (capsOn) "⇪" else "⇧"
            ModKind.Ctrl -> "Ctrl"
            ModKind.Alt -> "Alt"
            ModKind.Caps -> "Caps"
        }
        is SoftKey.Layer -> if (key.toSymbols) "?123" else "ABC"
        SoftKey.Space -> "Space"
        SoftKey.Enter -> "↵"
        SoftKey.Backspace -> "⌫"
        SoftKey.Tab -> "Tab"
        SoftKey.Esc -> "Esc"
    }

    private fun onSoftKey(key: SoftKey) {
        when (key) {
            is SoftKey.Modifier -> when (key.kind) {
                ModKind.Shift -> onShiftTap()
                ModKind.Ctrl -> {
                    ctrlOn = !ctrlOn
                    rebuild()
                }
                ModKind.Alt -> {
                    altOn = !altOn
                    rebuild()
                }
                ModKind.Caps -> Unit
            }
            is SoftKey.Layer -> {
                symbolsLayer = key.toSymbols
                rebuild()
            }
            SoftKey.Space -> emit(KeyCode.SPACE.usage, clearShift = false)
            SoftKey.Enter -> emit(KeyCode.ENTER.usage, clearShift = false)
            SoftKey.Backspace -> emit(KeyCode.BACKSPACE.usage, clearShift = false)
            SoftKey.Tab -> emit(KeyCode.TAB.usage, clearShift = false)
            SoftKey.Esc -> emit(KeyCode.ESC.usage, clearShift = false)
            is SoftKey.Usage -> {
                var mods = stickyMods()
                val applyShift = when {
                    key.forceShift -> true
                    key.isLetter && capsOn && shiftOn -> false
                    key.isLetter && capsOn -> true
                    shiftOn -> true
                    else -> false
                }
                if (applyShift) mods = mods or HidReports.MOD_LEFT_SHIFT
                emit(key.usage, mods, clearShift = !capsOn && !key.forceShift)
            }
        }
    }

    /** Tek basış = Shift; çift basış = Caps kilidi. */
    private fun onShiftTap() {
        val now = System.currentTimeMillis()
        if (now - lastShiftTapMs < 350) {
            capsOn = !capsOn
            shiftOn = false
        } else if (capsOn) {
            capsOn = false
            shiftOn = false
        } else {
            shiftOn = !shiftOn
        }
        lastShiftTapMs = now
        rebuild()
    }

    private fun stickyMods(): Int {
        var m = 0
        if (ctrlOn) m = m or HidReports.MOD_LEFT_CTRL
        if (altOn) m = m or HidReports.MOD_LEFT_ALT
        return m
    }

    private fun emit(usage: Int, modifiers: Int = stickyMods(), clearShift: Boolean) {
        listener?.onKey(usage, modifiers)
        if (clearShift && shiftOn) {
            shiftOn = false
            rebuild()
        }
    }

    private fun emit(usage: Int, clearShift: Boolean) {
        emit(usage, stickyMods(), clearShift)
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            resources.displayMetrics
        ).toInt()
}
