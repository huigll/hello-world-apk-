package com.example.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

/**
 * A very small candidate bar (no RecyclerView) for IME-like suggestions.
 */
class CandidateBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : HorizontalScrollView(context, attrs) {

    private val container: LinearLayout

    init {
        isHorizontalScrollBarEnabled = false
        LayoutInflater.from(context).inflate(R.layout.candidate_bar, this, true)
        container = findViewById(R.id.candidates_container)
    }

    fun setCandidates(candidates: List<String>, onClick: (index: Int, text: String) -> Unit) {
        container.removeAllViews()
        candidates.forEachIndexed { index, cand ->
            val btn = Button(context).apply {
                text = cand
                isAllCaps = false
                setOnClickListener { onClick(index, cand) }
            }
            container.addView(btn)
        }
    }

    fun clear() {
        container.removeAllViews()
    }
}
