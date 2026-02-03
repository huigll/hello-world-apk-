package com.carbit.inappkeyboard.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * A convenience composite view: CandidateBarView + InAppKeyboardView.
 *
 * Goal: make it trivial for consumers to drop a single view into XML and bind it to an EditText.
 */
public class InAppKeyboardPanelView extends LinearLayout {

    private final CandidateBarView candidateBarView;
    private final InAppKeyboardView keyboardView;

    public InAppKeyboardPanelView(Context context) {
        this(context, null);
    }

    public InAppKeyboardPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InAppKeyboardPanelView);
        int candidateBarHeightPx = a.getDimensionPixelSize(R.styleable.InAppKeyboardPanelView_candidateBarHeight, dp(50));
        boolean autoShowOnFocus = a.getBoolean(R.styleable.InAppKeyboardPanelView_autoShowOnFocus, true);
        boolean autoHideOnBlur = a.getBoolean(R.styleable.InAppKeyboardPanelView_autoHideOnBlur, true);
        a.recycle();

        this.candidateBarView = new CandidateBarView(context);
        candidateBarView.setId(R.id.candidate_bar_view);
        candidateBarView.setVisibility(View.INVISIBLE);

        FrameLayout keyboardContainer = new FrameLayout(context);
        keyboardContainer.setId(R.id.main_keyboard_container);
        keyboardContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        this.keyboardView = new InAppKeyboardView(context);
        keyboardView.setId(R.id.keyboard_view);
        keyboardView.setVisibility(View.INVISIBLE);
        keyboardView.ensureBuilt();
        FrameLayout.LayoutParams kvParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        keyboardContainer.addView(keyboardView, kvParams);

        addView(candidateBarView, new LayoutParams(LayoutParams.MATCH_PARENT, candidateBarHeightPx));
        addView(keyboardContainer);

        this.autoShowOnFocus = autoShowOnFocus;
        this.autoHideOnBlur = autoHideOnBlur;
    }

    private boolean autoShowOnFocus = true;
    private boolean autoHideOnBlur = true;

    public CandidateBarView getCandidateBarView() {
        return candidateBarView;
    }

    public InAppKeyboardView getKeyboardView() {
        return keyboardView;
    }

    public InAppKeyboardView.InputMode getInputMode() {
        return keyboardView.getInputMode();
    }

    public void setInputMode(InAppKeyboardView.InputMode value) {
        keyboardView.setInputMode(value);
    }

    public boolean isAutoShowOnFocus() {
        return autoShowOnFocus;
    }

    public void setAutoShowOnFocus(boolean autoShowOnFocus) {
        this.autoShowOnFocus = autoShowOnFocus;
    }

    public boolean isAutoHideOnBlur() {
        return autoHideOnBlur;
    }

    public void setAutoHideOnBlur(boolean autoHideOnBlur) {
        this.autoHideOnBlur = autoHideOnBlur;
    }

    public void bindTo(EditText editText) {
        editText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                attachTo(editText);
                if (autoShowOnFocus) show();
            }
        });
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    attachTo(editText);
                    if (autoShowOnFocus) show();
                } else {
                    if (autoHideOnBlur) hide();
                }
            }
        });
    }

    public void attachTo(EditText editText) {
        keyboardView.attachTo(editText, candidateBarView);
    }

    public void show() {
        android.util.Log.d("InAppKeyboardPanelView", "show keyboard view=" + System.identityHashCode(keyboardView)
                + " childCount=" + keyboardView.getChildCount()
                + " size=" + keyboardView.getWidth() + "x" + keyboardView.getHeight());
        keyboardView.ensureBuilt();
        keyboardView.requestLayout();
        keyboardView.invalidate();
        keyboardView.setVisibility(View.VISIBLE);
        requestLayout();
    }

    public void hide() {
        keyboardView.setVisibility(View.INVISIBLE);
        candidateBarView.clear();
    }

    public void release() {
        keyboardView.release();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
