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

    /**
     * Supported layouts (no candidate/suggestion system).
     *
     * - EN: QWERTY
     * - ZH: Pinyin mode (same letters as EN, different label + can add CN punctuation later)
     * - FR: AZERTY
     * - AR: Arabic basic
     */
    enum class Layout { EN, ZH_PINYIN, FR, AR, SYMBOLS }

    var target: Editable? = null

    var currentLayout: Layout = Layout.EN
        private set

    /** Notifies host to adjust target view direction (e.g. RTL for Arabic). */
    var onLayoutChanged: ((Layout) -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        rebuild()
    }

    fun attachTarget(editable: Editable?) {
        target = editable
    }

    fun setLayout(layout: Layout) {
        if (currentLayout == layout) return
        currentLayout = layout
        shift = false
        rebuild()
        onLayoutChanged?.invoke(currentLayout)
    }

    private fun rebuild() {
        removeAllViews()
        when (currentLayout) {
            Layout.EN -> buildEnQwerty()
            Layout.ZH_PINYIN -> buildZhPinyin()
            Layout.FR -> buildFrAzerty()
            Layout.AR -> buildArabic()
            Layout.SYMBOLS -> buildSymbols()
        }
    }

    private fun buildEnQwerty() {
        buildFromAskXml("ask_layouts/en_qwerty.xml")
    }

    private fun buildZhPinyin() {
        // Pinyin uses latin letters. We reuse the English layout.
        buildFromAskXml("ask_layouts/en_qwerty.xml")
    }

    private fun buildFrAzerty() {
        buildFromAskXml("ask_layouts/fr_azerty.xml")
    }

    private fun buildArabic() {
        buildFromAskXml("ask_layouts/ar_qwerty.xml")
    }

    private fun buildSymbols() {
        addRow(listOf("1","2","3","4","5","6","7","8","9","0"))
        addRow(listOf("@","#","$","%","&","*","-","+","(",")"))
        addRow(listOf("abc","_","\"","'",":",";","!","?","⌫"))
        addRow(listOf("lang","space","enter"))
    }

    private fun buildFromAskXml(assetPath: String) {
        // Parse ASK xml keyboard and render keys.
        val layout = AskXmlKeyboardParser.parseAsset(context.assets, assetPath)

        layout.rows.forEach { row ->
            // Convert ASK key definitions into our labels.
            val labels = row.mapNotNull { key ->
                when (key.code) {
                    -1 -> "⇧"
                    -5 -> "⌫"
                    null -> null
                    else -> {
                        // Prefer explicit label when provided.
                        key.label ?: key.code.toChar().toString()
                    }
                }
            }
            if (labels.isNotEmpty()) addRow(labels)
        }

        // Add our bottom utility row.
        addRow(listOf("lang","123","space","enter"))
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
                    "123" -> "123"
                    "lang" -> when (currentLayout) {
                        Layout.EN -> "EN"
                        Layout.ZH_PINYIN -> "中"
                        Layout.FR -> "FR"
                        Layout.AR -> "AR"
                        Layout.SYMBOLS -> "#"
                    }
                    else -> label
                }
                isAllCaps = false
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                setOnClickListener { onKey(label) }
            }

            // Make space wider in last row layouts
            if (label == "space") {
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
                setLayout(Layout.SYMBOLS)
            }
            "abc" -> {
                // Go back to English by default.
                setLayout(Layout.EN)
            }
            "lang" -> {
                // Cycle EN -> ZH(Pinyin) -> FR -> AR -> EN ...
                val next = when (currentLayout) {
                    Layout.EN -> Layout.ZH_PINYIN
                    Layout.ZH_PINYIN -> Layout.FR
                    Layout.FR -> Layout.AR
                    Layout.AR -> Layout.EN
                    Layout.SYMBOLS -> Layout.EN
                }
                setLayout(next)
            }
            else -> {
                val ch = if (shift) label.uppercase() else label
                editable.append(ch)
                // One-shot shift
                if (shift) shift = false
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
