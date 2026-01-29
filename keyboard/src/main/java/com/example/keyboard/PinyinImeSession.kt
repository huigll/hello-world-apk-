package com.example.keyboard

import android.widget.EditText

/**
 * Keeps pinyin composing state + candidate list and commits to a target EditText.
 *
 * This is not a system IME. It is designed for in-app use.
 */
class PinyinImeSession(private val decoder: PinyinDecoder) {

    private val composing = StringBuilder()

    fun clear() {
        composing.setLength(0)
        decoder.reset()
    }

    fun hasComposing(): Boolean = composing.isNotEmpty()

    fun composingText(): String = composing.toString()

    fun onCommitChar(ch: String, candidateBar: CandidateBarView) {
        composing.append(ch)
        refresh(candidateBar)
    }

    fun onBackspace(candidateBar: CandidateBarView): Boolean {
        if (composing.isEmpty()) return false
        composing.setLength(composing.length - 1)
        refresh(candidateBar)
        return true
    }

    fun onSpaceCommitBest(target: EditText, candidateBar: CandidateBarView): Boolean {
        if (composing.isEmpty()) return false
        val list = decoder.candidates(composing.toString(), max = 1)
        val commit = list.firstOrNull() ?: composing.toString()
        target.text.insert(target.selectionStart.coerceAtLeast(target.text.length), commit)
        clear()
        candidateBar.clear()
        return true
    }

    fun bindCandidateClicks(target: EditText, candidateBar: CandidateBarView) {
        val list = decoder.candidates(composing.toString(), max = 10)
        candidateBar.setCandidates(list) { index, _ ->
            val commit = decoder.choose(index)
            target.text.insert(target.selectionStart.coerceAtLeast(target.text.length), commit)
            clear()
            candidateBar.clear()
        }
    }

    private fun refresh(candidateBar: CandidateBarView) {
        if (composing.isEmpty()) {
            candidateBar.clear()
            return
        }
        // Rebuild buttons each time (simple PoC).
        // Host must call bindCandidateClicks(target, candidateBar) to wire commits.
        candidateBar.setCandidates(decoder.candidates(composing.toString(), max = 10)) { _, _ -> }
    }
}
