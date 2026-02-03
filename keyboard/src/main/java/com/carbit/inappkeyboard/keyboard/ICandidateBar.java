package com.carbit.inappkeyboard.keyboard;

import java.util.List;

/**
 * A very small candidate bar (no RecyclerView) for IME-like suggestions.
 */
public interface ICandidateBar {

    interface OnCandidateClickListener {
        void onClick(int index, String text);
    }

    void setCandidates(List<String> candidates, OnCandidateClickListener onClick);

    void clear();
}
