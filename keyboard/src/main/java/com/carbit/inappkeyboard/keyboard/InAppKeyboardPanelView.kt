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
 * Goal: make it trivial for consumers to drop a single view into XML and call [attachTo].
 */
class InAppKeyboardPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    val candidateBarView: CandidateBarView
    val keyboardView: InAppKeyboardView

    init {
        orientation = VERTICAL

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
            LayoutParams(LayoutParams.MATCH_PARENT, dp(50)),
        )
        addView(keyboardContainer)
    }

    /**
     * Attach keyboard to the given [EditText].
     *
     * This will:
     * - disable system IME (via InAppKeyboardView.attachTo)
     * - wire pinyin candidate bar when applicable
     */
    fun attachTo(editText: EditText) {
        keyboardView.attachTo(editText, candidateBarView)
        keyboardView.visibility = View.VISIBLE
    }

    fun hideKeyboard() {
        keyboardView.visibility = View.GONE
        candidateBarView.clear()
    }

    fun release() {
        keyboardView.release()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
