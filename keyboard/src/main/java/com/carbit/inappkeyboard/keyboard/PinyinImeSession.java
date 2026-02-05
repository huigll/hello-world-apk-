package com.carbit.inappkeyboard.keyboard;

import java.util.ArrayList;
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
        final String raw = composing.toString();
        final List<String> list = withRawCandidate(raw, decoder.candidates(raw, 10));
        candidateBar.setCandidates(list, new ICandidateBar.OnCandidateClickListener() {
            @Override
            public void onClick(int index, String text) {
                // index=0 is the raw pinyin letters; others map to decoder candidates.
                String commit;
                if (index == 0) {
                    commit = raw;
                } else {
                    commit = decoder.choose(index - 1);
                }
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
        final String raw = composing.toString();
        candidateBar.setCandidates(withRawCandidate(raw, decoder.candidates(raw, 10)), new ICandidateBar.OnCandidateClickListener() {
            @Override
            public void onClick(int index, String text) {
                // no-op for refresh (host wires commits via bindCandidateClicks)
            }
        });
    }

    private static List<String> withRawCandidate(String raw, List<String> candidates) {
        ArrayList<String> out = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) out.add(raw);
        if (candidates != null && !candidates.isEmpty()) out.addAll(candidates);
        return out;
    }
}
