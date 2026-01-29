package com.example.helloworld

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

/**
 * A minimal in-app (non-IME) soft keyboard.
 *
 * Key idea: we do NOT rely on Android's InputMethodService, so this works inside a Presentation
 * shown on a VirtualDisplay (where the system IME often won't appear).
 */
class InAppKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    enum class Layout { EN_QWERTY, SYMBOLS }

    var target: Editable? = null

    var currentLayout: Layout = Layout.EN_QWERTY
        private set

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        rebuild()
    }

    fun attachTarget(editable: Editable?) {
        target = editable
    }

    private fun rebuild() {
        removeAllViews()
        when (currentLayout) {
            Layout.EN_QWERTY -> buildQwerty()
            Layout.SYMBOLS -> buildSymbols()
        }
    }

    private fun buildQwerty() {
        addRow(listOf("q","w","e","r","t","y","u","i","o","p"))
        addRow(listOf("a","s","d","f","g","h","j","k","l"))
        addRow(listOf("⇧","z","x","c","v","b","n","m","⌫"))
        addRow(listOf("123","space","enter"))
    }

    private fun buildSymbols() {
        addRow(listOf("1","2","3","4","5","6","7","8","9","0"))
        addRow(listOf("@","#","$","%","&","*","-","+","(",")"))
        addRow(listOf("abc","_","\"","'",":",";","!","?","⌫"))
        addRow(listOf("space","enter"))
    }

    private fun addRow(keys: List<String>) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        keys.forEach { label ->
            val btn = Button(context).apply {
                text = when (label) {
                    "space" -> "Space"
                    "enter" -> "Enter"
                    "abc" -> "ABC"
                    else -> label
                }
                isAllCaps = false
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                setOnClickListener { onKey(label) }
            }

            // Make space wider in qwerty last row
            if (label == "space" && keys.size == 3) {
                btn.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 3f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
            }

            row.addView(btn)
        }

        addView(row)
    }

    private var shift = false

    private fun onKey(label: String) {
        val editable = target ?: return
        when (label) {
            "⌫" -> {
                val len = editable.length
                if (len > 0) editable.delete(len - 1, len)
            }
            "enter" -> {
                editable.append("\n")
            }
            "space" -> {
                editable.append(" ")
            }
            "⇧" -> {
                shift = !shift
            }
            "123" -> {
                currentLayout = Layout.SYMBOLS
                rebuild()
            }
            "abc" -> {
                currentLayout = Layout.EN_QWERTY
                rebuild()
            }
            else -> {
                val ch = if (shift) label.uppercase() else label
                editable.append(ch)
                // Common soft-keyboard behavior: one-shot shift
                if (shift) shift = false
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
