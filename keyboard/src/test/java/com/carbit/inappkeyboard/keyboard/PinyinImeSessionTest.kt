package com.carbit.inappkeyboard.keyboard

import org.junit.Assert.*
import org.junit.Test

private class FakeDecoder : IPinyinDecoder {
    var resetCalls = 0
    var lastPinyin: String? = null

    override fun reset() {
        resetCalls++
    }

    override fun candidates(pinyin: String, max: Int): List<String> {
        lastPinyin = pinyin
        // deterministic fake candidates
        val base = listOf("你", "拟", "尼", "呢", "泥")
        return base.take(max)
    }

    override fun choose(index: Int): String {
        return candidates(lastPinyin ?: "", 10).getOrElse(index) { "" }
    }
}

private class FakeCandidateBar : ICandidateBar {
    var lastCandidates: List<String> = emptyList()
    var cleared = false
    var lastOnClick: ((Int, String) -> Unit)? = null

    override fun setCandidates(candidates: List<String>, onClick: (index: Int, text: String) -> Unit) {
        cleared = false
        lastCandidates = candidates
        lastOnClick = onClick
    }

    override fun clear() {
        cleared = true
        lastCandidates = emptyList()
        lastOnClick = null
    }
}

private class BufferTarget : ITextCommitTarget {
    val sb = StringBuilder()
    override fun insert(text: String) {
        sb.append(text)
    }
}

class PinyinImeSessionTest {

    @Test
    fun `typing in composing mode updates candidates but does not auto-commit`() {
        val decoder = FakeDecoder()
        val bar = FakeCandidateBar()
        val session = PinyinImeSession(decoder)

        session.onCommitChar("n", bar)
        assertTrue(session.hasComposing())
        assertEquals("n", session.composingText())
        assertEquals("n", decoder.lastPinyin)
        assertTrue(bar.lastCandidates.isNotEmpty())
        assertFalse(bar.cleared)
    }

    @Test
    fun `space commits best candidate and clears composing`() {
        val decoder = FakeDecoder()
        val bar = FakeCandidateBar()
        val target = BufferTarget()
        val session = PinyinImeSession(decoder)

        session.onCommitChar("n", bar)
        session.onCommitChar("i", bar)
        val consumed = session.onSpaceCommitBest(target, bar)

        assertTrue(consumed)
        assertFalse(session.hasComposing())
        assertEquals("你", target.sb.toString())
        assertTrue(bar.cleared)
    }

    @Test
    fun `candidate click commits selected candidate and clears composing`() {
        val decoder = FakeDecoder()
        val bar = FakeCandidateBar()
        val target = BufferTarget()
        val session = PinyinImeSession(decoder)

        session.onCommitChar("n", bar)
        session.onCommitChar("i", bar)
        session.bindCandidateClicks(target, bar)

        // simulate user clicking 3rd candidate
        val click = requireNotNull(bar.lastOnClick)
        click(2, bar.lastCandidates[2])

        assertEquals("尼", target.sb.toString())
        assertFalse(session.hasComposing())
        assertTrue(bar.cleared)
    }

    @Test
    fun `backspace in composing mode consumes and updates candidates`() {
        val decoder = FakeDecoder()
        val bar = FakeCandidateBar()
        val session = PinyinImeSession(decoder)

        session.onCommitChar("n", bar)
        session.onCommitChar("i", bar)
        val consumed = session.onBackspace(bar)

        assertTrue(consumed)
        assertEquals("n", session.composingText())
        assertTrue(bar.lastCandidates.isNotEmpty())
    }
}
