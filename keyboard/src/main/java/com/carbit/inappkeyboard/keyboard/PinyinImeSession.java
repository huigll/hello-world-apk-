package com.carbit.inappkeyboard.keyboard;

import java.util.List;

/**
 * Keeps pinyin composing state + candidate list and commits to a target.
 * This is not a system IME. It is designed for in-app use.
 */
public class PinyinImeSession {

    private final IPinyinDecoder decoder;
    private final StringBuilder composing = new StringBuilder();

    public PinyinImeSession(IPinyinDecoder decoder) {
        this.decoder = decoder;
    }

    public void clear() {
        composing.setLength(0);
        decoder.reset();
    }

    public boolean hasComposing() {
        return composing.length() > 0;
    }

    public String composingText() {
        return composing.toString();
    }

    public void onCommitChar(String ch, ICandidateBar candidateBar) {
        composing.append(ch);
        refresh(candidateBar);
    }

    public boolean onBackspace(ICandidateBar candidateBar) {
        if (composing.length() == 0) return false;
        composing.setLength(composing.length() - 1);
        refresh(candidateBar);
        return true;
    }

    public boolean onSpaceCommitBest(ITextCommitTarget target, ICandidateBar candidateBar) {
        if (composing.length() == 0) return false;
        List<String> list = decoder.candidates(composing.toString(), 1);
        String commit = list.isEmpty() ? composing.toString() : list.get(0);
        target.insert(commit);
        clear();
        candidateBar.clear();
        return true;
    }

    public void bindCandidateClicks(final ITextCommitTarget target, ICandidateBar candidateBar) {
        List<String> list = decoder.candidates(composing.toString(), 10);
        candidateBar.setCandidates(list, new ICandidateBar.OnCandidateClickListener() {
            @Override
            public void onClick(int index, String text) {
                String commit = decoder.choose(index);
                target.insert(commit);
                clear();
                candidateBar.clear();
            }
        });
    }

    private void refresh(ICandidateBar candidateBar) {
        if (composing.length() == 0) {
            candidateBar.clear();
            return;
        }
        candidateBar.setCandidates(decoder.candidates(composing.toString(), 10), new ICandidateBar.OnCandidateClickListener() {
            @Override
            public void onClick(int index, String text) {
                // no-op for refresh (host wires commits via bindCandidateClicks)
            }
        });
    }
}
