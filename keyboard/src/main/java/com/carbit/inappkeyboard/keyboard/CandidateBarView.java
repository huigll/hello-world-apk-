package com.carbit.inappkeyboard.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.List;

/**
 * A very small candidate bar (no RecyclerView) for IME-like suggestions.
 */
public class CandidateBarView extends HorizontalScrollView implements ICandidateBar {

    private final LinearLayout container;

    public CandidateBarView(Context context) {
        this(context, null);
    }

    public CandidateBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setHorizontalScrollBarEnabled(false);
        LayoutInflater.from(context).inflate(R.layout.candidate_bar, this, true);
        container = findViewById(R.id.candidates_container);
    }

    @Override
    public void setCandidates(List<String> candidates, final OnCandidateClickListener listener) {
        container.removeAllViews();

        if (candidates == null || candidates.isEmpty()) {
            setVisibility(INVISIBLE);
            return;
        }

        for (int index = 0; index < candidates.size(); index++) {
            final String cand = candidates.get(index);
            final int idx = index;
            Button btn = new Button(getContext());
            btn.setText(cand);
            btn.setAllCaps(false);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(idx, cand);
                }
            });
            container.addView(btn);
        }

        setVisibility(VISIBLE);
    }

    @Override
    public void clear() {
        container.removeAllViews();
        setVisibility(INVISIBLE);
    }
}
