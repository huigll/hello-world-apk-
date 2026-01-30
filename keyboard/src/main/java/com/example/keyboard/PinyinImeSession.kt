package com.example.keyboard

/**
 * Keeps pinyin composing state + candidate list and commits to a target.
 *
 * This is not a system IME. It is designed for in-app use.
 */
interface ITextCommitTarget {
    /** Insert text at current cursor (implementation decides exact placement). */
    fun insert(text: String)
}

class PinyinImeSession(private val decoder: IPinyinDecoder) {

    private val composing = StringBuilder()

    fun clear() {
        composing.setLength(0)
        decoder.reset()
    }

    fun hasComposing(): Boolean = composing.isNotEmpty()

    fun composingText(): String = composing.toString()

    fun onCommitChar(ch: String, candidateBar: ICandidateBar) {
        composing.append(ch)
        refresh(candidateBar)
    }

    fun onBackspace(candidateBar: ICandidateBar): Boolean {
        if (composing.isEmpty()) return false
        composing.setLength(composing.length - 1)
        refresh(candidateBar)
        return true
    }

    fun onSpaceCommitBest(target: ITextCommitTarget, candidateBar: ICandidateBar): Boolean {
        if (composing.isEmpty()) return false
        val list = decoder.candidates(composing.toString(), max = 1)
        val commit = list.firstOrNull() ?: composing.toString()
        target.insert(commit)
        clear()
        candidateBar.clear()
        return true
    }

    fun bindCandidateClicks(target: ITextCommitTarget, candidateBar: ICandidateBar) {
        val list = decoder.candidates(composing.toString(), max = 10)
        candidateBar.setCandidates(list) { index, _ ->
            val commit = decoder.choose(index)
            target.insert(commit)
            clear()
            candidateBar.clear()
        }
    }

    private fun refresh(candidateBar: ICandidateBar) {
        if (composing.isEmpty()) {
            candidateBar.clear()
            return
        }
        // Rebuild buttons each time (simple PoC).
        // Host must call bindCandidateClicks(target, candidateBar) to wire commits.
        candidateBar.setCandidates(decoder.candidates(composing.toString(), max = 10)) { _, _ -> }
    }
}
