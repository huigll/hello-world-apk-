package com.carbit.inappkeyboard.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * A convenience composite view: CandidateBarView + InAppKeyboardView.
 *
 * Goal: make it trivial for consumers to drop a single view into XML and bind it to an EditText.
 */
class InAppKeyboardPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    val candidateBarView: CandidateBarView
    val keyboardView: InAppKeyboardView

    /** Convenience proxy. */
    var inputMode: InAppKeyboardView.InputMode
        get() = keyboardView.inputMode
        set(value) {
            keyboardView.inputMode = value
        }

    var autoShowOnFocus: Boolean = true
    var autoHideOnBlur: Boolean = true

    init {
        orientation = VERTICAL

        val a = context.obtainStyledAttributes(attrs, R.styleable.InAppKeyboardPanelView)
        val candidateBarHeightPx = a.getDimensionPixelSize(
            R.styleable.InAppKeyboardPanelView_candidateBarHeight,
            dp(50),
        )
        autoShowOnFocus = a.getBoolean(R.styleable.InAppKeyboardPanelView_autoShowOnFocus, true)
        autoHideOnBlur = a.getBoolean(R.styleable.InAppKeyboardPanelView_autoHideOnBlur, true)
        a.recycle()

        candidateBarView = CandidateBarView(context).apply {
            id = R.id.candidate_bar_view
            visibility = View.INVISIBLE
        }

        val keyboardContainer = FrameLayout(context).apply {
            id = R.id.main_keyboard_container
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        keyboardView = InAppKeyboardView(context).apply {
            id = R.id.keyboard_view
            visibility = View.GONE
        }
        keyboardContainer.addView(keyboardView)

        addView(
            candidateBarView,
            LayoutParams(LayoutParams.MATCH_PARENT, candidateBarHeightPx),
        )
        addView(keyboardContainer)
    }

    /**
     * Bind this panel to an [EditText].
     *
     * Default behavior:
     * - click/focus => attach (+ show if [autoShowOnFocus])
     * - losing focus => hide if [autoHideOnBlur]
     */
    fun bindTo(editText: EditText) {
        editText.setOnClickListener {
            attachTo(editText)
            if (autoShowOnFocus) show()
        }
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                attachTo(editText)
                if (autoShowOnFocus) show()
            } else {
                if (autoHideOnBlur) hide()
            }
        }
    }

    /** Attach keyboard logic to the given [EditText] (does not change visibility). */
    fun attachTo(editText: EditText) {
        keyboardView.attachTo(editText, candidateBarView)
    }

    fun show() {
        keyboardView.visibility = View.VISIBLE
    }

    fun hide() {
        keyboardView.visibility = View.GONE
        candidateBarView.clear()
    }

    fun release() {
        keyboardView.release()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
