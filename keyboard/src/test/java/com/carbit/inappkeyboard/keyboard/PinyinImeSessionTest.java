package com.carbit.inappkeyboard.keyboard;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PinyinImeSessionTest {

    private static class FakeDecoder implements IPinyinDecoder {
        int resetCalls = 0;
        String lastPinyin = null;

        @Override
        public void reset() {
            resetCalls++;
        }

        @Override
        public List<String> candidates(String pinyin, int max) {
            lastPinyin = pinyin;
            List<String> base = Arrays.asList("你", "拟", "尼", "呢", "泥");
            return base.subList(0, Math.min(max, base.size()));
        }

        @Override
        public String choose(int index) {
            List<String> c = candidates(lastPinyin != null ? lastPinyin : "", 10);
            return index >= 0 && index < c.size() ? c.get(index) : "";
        }
    }

    private static class FakeCandidateBar implements ICandidateBar {
        List<String> lastCandidates = Arrays.asList();
        boolean cleared = false;
        OnCandidateClickListener lastOnClick = null;

        @Override
        public void setCandidates(List<String> candidates, OnCandidateClickListener onClick) {
            cleared = false;
            lastCandidates = candidates != null ? candidates : Arrays.<String>asList();
            lastOnClick = onClick;
        }

        @Override
        public void clear() {
            cleared = true;
            lastCandidates = Arrays.asList();
            lastOnClick = null;
        }
    }

    private static class BufferTarget implements ITextCommitTarget {
        final StringBuilder sb = new StringBuilder();

        @Override
        public void insert(String text) {
            sb.append(text);
        }
    }

    @Test
    public void typing_in_composing_mode_updates_candidates_but_does_not_auto_commit() {
        FakeDecoder decoder = new FakeDecoder();
        FakeCandidateBar bar = new FakeCandidateBar();
        PinyinImeSession session = new PinyinImeSession(decoder);

        session.onCommitChar("n", bar);
        assertTrue(session.hasComposing());
        assertEquals("n", session.composingText());
        assertEquals("n", decoder.lastPinyin);
        assertFalse(bar.lastCandidates.isEmpty());
        assertFalse(bar.cleared);
    }

    @Test
    public void space_commits_best_candidate_and_clears_composing() {
        FakeDecoder decoder = new FakeDecoder();
        FakeCandidateBar bar = new FakeCandidateBar();
        BufferTarget target = new BufferTarget();
        PinyinImeSession session = new PinyinImeSession(decoder);

        session.onCommitChar("n", bar);
        session.onCommitChar("i", bar);
        boolean consumed = session.onSpaceCommitBest(target, bar);

        assertTrue(consumed);
        assertFalse(session.hasComposing());
        assertEquals("你", target.sb.toString());
        assertTrue(bar.cleared);
    }

    @Test
    public void candidate_click_commits_selected_candidate_and_clears_composing() {
        FakeDecoder decoder = new FakeDecoder();
        FakeCandidateBar bar = new FakeCandidateBar();
        BufferTarget target = new BufferTarget();
        PinyinImeSession session = new PinyinImeSession(decoder);

        session.onCommitChar("n", bar);
        session.onCommitChar("i", bar);
        session.bindCandidateClicks(target, bar);

        assertNotNull(bar.lastOnClick);
        bar.lastOnClick.onClick(2, bar.lastCandidates.get(2));

        assertEquals("尼", target.sb.toString());
        assertFalse(session.hasComposing());
        assertTrue(bar.cleared);
    }

    @Test
    public void backspace_in_composing_mode_consumes_and_updates_candidates() {
        FakeDecoder decoder = new FakeDecoder();
        FakeCandidateBar bar = new FakeCandidateBar();
        PinyinImeSession session = new PinyinImeSession(decoder);

        session.onCommitChar("n", bar);
        session.onCommitChar("i", bar);
        boolean consumed = session.onBackspace(bar);

        assertTrue(consumed);
        assertEquals("n", session.composingText());
        assertFalse(bar.lastCandidates.isEmpty());
    }
}
