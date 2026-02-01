package com.carbit.inappkeyboard.keyboard

import android.content.Context
import android.content.res.Configuration
import android.text.Editable
import android.text.InputType
import android.util.AttributeSet
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
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
    enum class Layout { EN, ZH_PINYIN, FR, AR, SYMBOLS, NUMERIC }

    enum class InputMode { AUTO, TEXT, NUMBER, PASSWORD }

    private var inputModeLocked: Boolean = false
    private var _inputMode: InputMode = InputMode.AUTO

    /**
     * Input mode policy.
     *
     * - Default is [AUTO].
     * - If you set this to a non-AUTO value, it becomes "locked" and [attachTo] will not override it.
     * - If you set it back to AUTO, [attachTo] will infer mode from EditText.inputType.
     */
    var inputMode: InputMode
        get() = _inputMode
        set(value) {
            _inputMode = value
            inputModeLocked = value != InputMode.AUTO
            applyInputModeIfNeeded()
        }

    private fun setInferredInputMode(value: InputMode) {
        // Internal helper: inferred from EditText.inputType; should not lock.
        _inputMode = value
        inputModeLocked = false
        applyInputModeIfNeeded()
    }

    var target: Editable? = null

    private var boundEditText: EditText? = null
    private var candidateBar: ICandidateBar? = null

    // Optional built-in pinyin wiring (only used when attachTo(..., candidateBar=...)).
    private var pinyinDecoder: PinyinDecoder? = null
    private var pinyinSession: PinyinImeSession? = null

    private var isLandscape = false
    private var isUltraWide = false

    var currentLayout: Layout = Layout.EN
        private set

    /** Notifies host to adjust target view direction (e.g. RTL for Arabic). */
    var onLayoutChanged: ((Layout) -> Unit)? = null

    /**
     * Optional interception hooks.
     * Return true to consume and prevent default Editable writes.
     */
    var onCommitText: ((layout: Layout, text: String) -> Boolean)? = null
    var onBackspace: ((layout: Layout) -> Boolean)? = null
    var onSpace: ((layout: Layout) -> Boolean)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        updateSizeFlags(width, height)
        rebuild()
    }

    /**
     * Lowest-level binding: just provide an [Editable] to receive committed characters.
     *
     * If you want auto input-type handling + optional pinyin candidate wiring, use [attachTo].
     */
    fun attachTarget(editable: Editable?) {
        target = editable
    }

    /**
     * High-level binding intended for library consumers.
     *
     * - Disables the system IME (showSoftInputOnFocus=false)
     * - Attaches target Editable
     * - Auto-selects a keyboard layout for NUMBER/PASSWORD/TEXT
     * - Optionally wires Pinyin composing + candidate commit when [candidateBar] is provided
     */
    fun attachTo(editText: EditText, candidateBar: ICandidateBar? = null) {
        boundEditText = editText
        this.candidateBar = candidateBar
        attachTarget(editText.text)

        // Prevent system IME.
        try {
            editText.showSoftInputOnFocus = false
        } catch (_: Throwable) {
            // ignore on older API
        }

        // Infer input mode from inputType unless consumer explicitly locked inputMode.
        if (!inputModeLocked) {
            setInferredInputMode(inferInputMode(editText.inputType))
        }

        wireBuiltInPinyinIfNeeded()
        applyInputModeIfNeeded()
    }

    /** Release JNI decoder resources if [attachTo] created them. */
    fun release() {
        try {
            pinyinDecoder?.close()
        } catch (_: Throwable) {
        }
        pinyinDecoder = null
        pinyinSession = null
    }

    fun setLayout(layout: Layout) {
        if (currentLayout == layout) return
        currentLayout = layout
        shift = false
        rebuild()
        onLayoutChanged?.invoke(currentLayout)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val beforeLandscape = isLandscape
        val beforeUltraWide = isUltraWide
        updateSizeFlags(w, h)
        if (beforeLandscape != isLandscape || beforeUltraWide != isUltraWide) {
            rebuild()
        }
    }

    private fun updateSizeFlags(w: Int, h: Int) {
        val cfg = resources.configuration
        isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE || (w > 0 && h > 0 && w > h)
        // "Ultra-wide" heuristic: very wide aspect ratio OR large width.
        val aspect = if (w > 0 && h > 0) w.toFloat() / h.toFloat() else 0f
        isUltraWide = aspect >= 2.0f || w >= dp(1000)
    }

    private fun inferInputMode(inputType: Int): InputMode {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val klass = inputType and InputType.TYPE_MASK_CLASS

        val isPassword =
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD

        if (isPassword) return InputMode.PASSWORD

        if (klass == InputType.TYPE_CLASS_NUMBER || klass == InputType.TYPE_CLASS_PHONE) {
            return InputMode.NUMBER
        }

        return InputMode.TEXT
    }

    private fun applyInputModeIfNeeded() {
        // Called when inputMode changes or when attachTo binds an EditText.
        when (inputMode) {
            InputMode.AUTO -> Unit // will be resolved in attachTo
            InputMode.NUMBER -> {
                // prefer numeric keypad
                if (currentLayout != Layout.NUMERIC) setLayout(Layout.NUMERIC)
            }
            InputMode.PASSWORD -> {
                // Avoid pinyin/candidates by default.
                // Always start from EN for password fields.
                if (currentLayout != Layout.EN) setLayout(Layout.EN)
                candidateBar?.clear()
            }
            InputMode.TEXT -> {
                // For normal text fields, avoid staying in numeric/symbol-only layouts.
                if (currentLayout == Layout.NUMERIC || currentLayout == Layout.SYMBOLS) {
                    setLayout(Layout.EN)
                }
            }
        }
    }

    private fun wireBuiltInPinyinIfNeeded() {
        val et = boundEditText ?: return
        val bar = candidateBar

        // Only wire pinyin when we have a candidate bar and the field isn't password/number.
        if (bar == null || inputMode != InputMode.TEXT) {
            pinyinSession = null
            try { pinyinDecoder?.close() } catch (_: Throwable) {}
            pinyinDecoder = null
            return
        }

        if (pinyinDecoder == null) {
            pinyinDecoder = PinyinDecoder(context)
        }
        if (pinyinSession == null) {
            pinyinSession = PinyinImeSession(pinyinDecoder!!)
        }

        val commitTarget = object : ITextCommitTarget {
            override fun insert(text: String) {
                et.text.insert(et.selectionStart.coerceAtLeast(et.text.length), text)
            }
        }

        fun refreshCandidates() {
            val session = pinyinSession ?: return
            if (!session.hasComposing()) {
                bar.clear()
                return
            }
            session.bindCandidateClicks(commitTarget, bar)
        }

        // NOTE: these callbacks remain overridable by host. We only provide sensible defaults.
        onCommitText = { layout, text ->
            val session = pinyinSession
            if (layout == Layout.ZH_PINYIN && session != null) {
                session.onCommitChar(text, bar)
                refreshCandidates()
                true
            } else {
                false
            }
        }

        onBackspace = { layout ->
            val session = pinyinSession
            if (layout == Layout.ZH_PINYIN && session != null) {
                val consumed = session.onBackspace(bar)
                if (consumed) refreshCandidates()
                consumed
            } else {
                false
            }
        }

        onSpace = { layout ->
            val session = pinyinSession
            if (layout == Layout.ZH_PINYIN && session != null) {
                val consumed = session.onSpaceCommitBest(commitTarget, bar)
                if (consumed) refreshCandidates()
                consumed
            } else {
                false
            }
        }

        // Clear pinyin state when leaving ZH mode.
        val existing = onLayoutChanged
        onLayoutChanged = { layout ->
            existing?.invoke(layout)
            val session = pinyinSession
            if (layout != Layout.ZH_PINYIN && session != null) {
                session.clear()
                bar.clear()
            }
        }
    }

    private fun rebuild() {
        removeAllViews()
        when (currentLayout) {
            Layout.EN -> buildEnQwerty()
            Layout.ZH_PINYIN -> buildZhPinyin()
            Layout.FR -> buildFrAzerty()
            Layout.AR -> buildArabic()
            Layout.SYMBOLS -> buildSymbols()
            Layout.NUMERIC -> buildNumeric()
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

    private fun buildNumeric() {
        // Simple numeric keypad-style layout.
        addRow(listOf("1","2","3"))
        addRow(listOf("4","5","6"))
        addRow(listOf("7","8","9"))
        addRow(listOf("123","0","⌫"))
        addRow(listOf("lang","enter"))
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

        val keyHeight = if (isLandscape) dp(36) else dp(44)
        val fixedKeyWidth = if (isUltraWide) dp(56) else 0

        keys.forEach { label ->
            val btn = Button(context).apply {
                text = when (label) {
                    "space" -> "Space"
                    "enter" -> "Enter"
                    "abc" -> "ABC"
                    "123" -> if (currentLayout == Layout.NUMERIC) "#+=" else "123"
                    "lang" -> when (currentLayout) {
                        Layout.EN -> "EN"
                        Layout.ZH_PINYIN -> "中"
                        Layout.FR -> "FR"
                        Layout.AR -> "AR"
                        Layout.SYMBOLS -> "#"
                        Layout.NUMERIC -> "123"
                    }
                    else -> label
                }
                isAllCaps = false
                minHeight = keyHeight
                minimumHeight = keyHeight
            }

            btn.layoutParams = if (isUltraWide) {
                // Fixed widths so keys don't become comically wide on ultra-wide screens.
                val w = when (label) {
                    "space" -> fixedKeyWidth * 4
                    "enter" -> fixedKeyWidth * 2
                    "lang" -> fixedKeyWidth * 2
                    else -> fixedKeyWidth
                }
                LayoutParams(w, LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
            } else {
                // Normal weight-based stretch for phones/tablets.
                val weight = when (label) {
                    "space" -> 3f
                    else -> 1f
                }
                LayoutParams(0, LayoutParams.WRAP_CONTENT, weight).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
            }

            if (label == "⌫") {
                // Long-press backspace = fast delete
                setupBackspaceRepeater(btn)
            } else {
                btn.setOnClickListener { onKey(label) }
            }

            row.addView(btn)
        }

        addView(row)
    }

    private var shift = false

    /**
     * Test/automation hook.
     *
     * Some devices do not expose custom keyboard children via UIAutomator dumps, and external ADB
     * tapping is flaky. Instrumentation tests should call this to drive the keyboard logic
     * deterministically.
     */
    @androidx.annotation.VisibleForTesting
    fun injectKey(label: String) {
        onKey(label)
    }

    private fun onKey(label: String) {
        val editable = target ?: return
        when (label) {
            "⌫" -> {
                val consumed = onBackspace?.invoke(currentLayout) ?: false
                if (!consumed) {
                    val len = editable.length
                    if (len > 0) editable.delete(len - 1, len)
                }
            }
            "enter" -> {
                editable.append("\n")
            }
            "space" -> {
                val consumed = onSpace?.invoke(currentLayout) ?: false
                if (!consumed) {
                    editable.append(" ")
                }
            }
            "⇧" -> {
                shift = !shift
            }
            "123" -> {
                // If we were in numeric keypad, allow switching to symbols; otherwise symbols.
                setLayout(Layout.SYMBOLS)
            }
            "abc" -> {
                // Go back based on inputMode.
                when (inputMode) {
                    InputMode.NUMBER -> setLayout(Layout.NUMERIC)
                    else -> setLayout(Layout.EN)
                }
            }
            "lang" -> {
                // In numeric/password mode, don't cycle languages.
                if (inputMode == InputMode.NUMBER) {
                    setLayout(Layout.NUMERIC)
                    return
                }
                if (inputMode == InputMode.PASSWORD) {
                    setLayout(Layout.EN)
                    return
                }

                // Cycle EN -> ZH(Pinyin) -> FR -> AR -> EN ...
                val next = when (currentLayout) {
                    Layout.EN -> Layout.ZH_PINYIN
                    Layout.ZH_PINYIN -> Layout.FR
                    Layout.FR -> Layout.AR
                    Layout.AR -> Layout.EN
                    Layout.SYMBOLS -> Layout.EN
                    Layout.NUMERIC -> Layout.EN
                }
                setLayout(next)
            }
            else -> {
                val ch = if (shift) label.uppercase() else label
                val consumed = onCommitText?.invoke(currentLayout, ch) ?: false
                if (!consumed) {
                    editable.append(ch)
                }
                // One-shot shift
                if (shift) shift = false
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun setupBackspaceRepeater(btn: Button) {
        val initialDelayMs = 250L
        val repeatDelayMs = 50L

        var repeating = false
        val repeatRunnable = object : Runnable {
            override fun run() {
                if (!repeating) return
                onKey("⌫")
                handler.postDelayed(this, repeatDelayMs)
            }
        }

        btn.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // single delete immediately
                    onKey("⌫")
                    repeating = true
                    handler.postDelayed(repeatRunnable, initialDelayMs)
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    repeating = false
                    handler.removeCallbacks(repeatRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
