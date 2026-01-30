package com.carbit.inappkeyboard.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

/**
 * A very small candidate bar (no RecyclerView) for IME-like suggestions.
 */
interface ICandidateBar {
    fun setCandidates(candidates: List<String>, onClick: (index: Int, text: String) -> Unit)
    fun clear()
}

class CandidateBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : HorizontalScrollView(context, attrs), ICandidateBar {

    private val container: LinearLayout

    init {
        isHorizontalScrollBarEnabled = false
        LayoutInflater.from(context).inflate(R.layout.candidate_bar, this, true)
        container = findViewById(R.id.candidates_container)
    }

    override fun setCandidates(candidates: List<String>, onClick: (index: Int, text: String) -> Unit) {
        container.removeAllViews()

        if (candidates.isEmpty()) {
            // Keep layout space (CandidateBarView should be INVISIBLE rather than GONE)
            visibility = INVISIBLE
            return
        }

        candidates.forEachIndexed { index, cand ->
            val btn = Button(context).apply {
                text = cand
                isAllCaps = false
                setOnClickListener { onClick(index, cand) }
            }
            container.addView(btn)
        }

        visibility = VISIBLE
    }

    override fun clear() {
        container.removeAllViews()
        visibility = INVISIBLE
    }
}
