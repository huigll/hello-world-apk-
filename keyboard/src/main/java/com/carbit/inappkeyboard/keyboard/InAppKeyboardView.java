package com.carbit.inappkeyboard.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.graphics.Color;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * A minimal in-app (non-IME) soft keyboard.
 *
 * Key idea: we do NOT rely on Android's InputMethodService, so this works inside a Presentation
 * shown on a VirtualDisplay (where the system IME often won't appear).
 */
public class InAppKeyboardView extends LinearLayout {
    private static final String TAG = "InAppKeyboardView";
    private static final boolean DEBUG = true;

    public enum Layout { EN, ZH_PINYIN, FR, AR, SYMBOLS, NUMERIC }

    public enum InputMode { AUTO, TEXT, NUMBER, PASSWORD }

    public interface OnLayoutChangedListener {
        void onLayoutChanged(Layout layout);
    }

    public interface OnCommitTextListener {
        boolean onCommitText(Layout layout, String text);
    }

    public interface OnBackspaceListener {
        boolean onBackspace(Layout layout);
    }

    public interface OnSpaceListener {
        boolean onSpace(Layout layout);
    }

    private boolean inputModeLocked = false;
    private InputMode inputMode = InputMode.AUTO;

    private Editable target;
    private EditText boundEditText;
    private ICandidateBar candidateBar;
    private PinyinDecoder pinyinDecoder;
    private PinyinImeSession pinyinSession;

    private boolean isLandscape = false;
    private boolean isUltraWide = false;

    private Layout currentLayout = Layout.EN;

    private OnLayoutChangedListener onLayoutChangedListener;
    private OnCommitTextListener onCommitTextListener;
    private OnBackspaceListener onBackspaceListener;
    private OnSpaceListener onSpaceListener;

    private boolean shift = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public InAppKeyboardView(Context context) {
        this(context, null);
    }

    public InAppKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        updateSizeFlags(getWidth(), getHeight());
        rebuild();
    }

    public InputMode getInputMode() {
        return inputMode;
    }

    public void setInputMode(InputMode value) {
        inputMode = value;
        inputModeLocked = (value != InputMode.AUTO);
        applyInputModeIfNeeded();
    }

    private void setInferredInputMode(InputMode value) {
        inputMode = value;
        inputModeLocked = false;
        applyInputModeIfNeeded();
    }

    public Editable getTarget() {
        return target;
    }

    public void setTarget(Editable target) {
        this.target = target;
    }

    public Layout getCurrentLayout() {
        return currentLayout;
    }

    public OnLayoutChangedListener getOnLayoutChangedListener() {
        return onLayoutChangedListener;
    }

    public void setOnLayoutChangedListener(OnLayoutChangedListener listener) {
        this.onLayoutChangedListener = listener;
    }

    public OnCommitTextListener getOnCommitTextListener() {
        return onCommitTextListener;
    }

    public void setOnCommitTextListener(OnCommitTextListener listener) {
        this.onCommitTextListener = listener;
    }

    public OnBackspaceListener getOnBackspaceListener() {
        return onBackspaceListener;
    }

    public void setOnBackspaceListener(OnBackspaceListener listener) {
        this.onBackspaceListener = listener;
    }

    public OnSpaceListener getOnSpaceListener() {
        return onSpaceListener;
    }

    public void setOnSpaceListener(OnSpaceListener listener) {
        this.onSpaceListener = listener;
    }

    public void attachTarget(Editable editable) {
        this.target = editable;
    }

    public void attachTo(EditText editText) {
        attachTo(editText, null);
    }

    public void attachTo(EditText editText, ICandidateBar candidateBar) {
        this.boundEditText = editText;
        this.candidateBar = candidateBar;
        attachTarget(editText.getText());

        try {
            editText.setShowSoftInputOnFocus(false);
        } catch (Throwable ignored) {
        }

        if (!inputModeLocked) {
            setInferredInputMode(inferInputMode(editText.getInputType()));
        }

        wireBuiltInPinyinIfNeeded();
        applyInputModeIfNeeded();
    }

    /** Ensure keys are built (e.g., after attachment/visibility changes). */
    public void ensureBuilt() {
        if (getChildCount() == 0) {
            rebuild();
        }
    }

    public void release() {
        try {
            if (pinyinDecoder != null) pinyinDecoder.close();
        } catch (Throwable ignored) {
        }
        pinyinDecoder = null;
        pinyinSession = null;
    }

    public void setLayout(Layout layout) {
        if (currentLayout == layout) return;
        currentLayout = layout;
        shift = false;
        rebuild();
        if (onLayoutChangedListener != null) onLayoutChangedListener.onLayoutChanged(currentLayout);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        boolean beforeLandscape = isLandscape;
        boolean beforeUltraWide = isUltraWide;
        updateSizeFlags(w, h);
        if (beforeLandscape != isLandscape || beforeUltraWide != isUltraWide) {
            rebuild();
        }
    }

    private void updateSizeFlags(int w, int h) {
        Configuration cfg = getResources().getConfiguration();
        isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE || (w > 0 && h > 0 && w > h);
        float aspect = (w > 0 && h > 0) ? (float) w / (float) h : 0f;
        isUltraWide = aspect >= 2.0f || w >= dp(1000);
    }

    private InputMode inferInputMode(int inputType) {
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        int klass = inputType & InputType.TYPE_MASK_CLASS;

        boolean isPassword =
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                        || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                        || variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;

        if (isPassword) return InputMode.PASSWORD;
        if (klass == InputType.TYPE_CLASS_NUMBER || klass == InputType.TYPE_CLASS_PHONE) {
            return InputMode.NUMBER;
        }
        return InputMode.TEXT;
    }

    private void applyInputModeIfNeeded() {
        switch (inputMode) {
            case AUTO:
                break;
            case NUMBER:
                if (currentLayout != Layout.NUMERIC) setLayout(Layout.NUMERIC);
                break;
            case PASSWORD:
                if (currentLayout != Layout.EN) setLayout(Layout.EN);
                if (candidateBar != null) candidateBar.clear();
                break;
            case TEXT:
                if (currentLayout == Layout.NUMERIC || currentLayout == Layout.SYMBOLS) {
                    setLayout(Layout.EN);
                }
                break;
        }
    }

    private void wireBuiltInPinyinIfNeeded() {
        final EditText et = boundEditText;
        final ICandidateBar bar = candidateBar;

        if (bar == null || inputMode != InputMode.TEXT) {
            pinyinSession = null;
            try {
                if (pinyinDecoder != null) pinyinDecoder.close();
            } catch (Throwable ignored) {
            }
            pinyinDecoder = null;
            return;
        }

        if (pinyinDecoder == null) pinyinDecoder = new PinyinDecoder(getContext());
        if (pinyinSession == null) pinyinSession = new PinyinImeSession(pinyinDecoder);

        final ITextCommitTarget commitTarget = new ITextCommitTarget() {
            @Override
            public void insert(String text) {
                int pos = Math.max(et.getSelectionStart(), et.getText().length());
                et.getText().insert(pos, text);
            }
        };

        final Runnable refreshCandidates = new Runnable() {
            @Override
            public void run() {
                if (pinyinSession == null) return;
                if (!pinyinSession.hasComposing()) {
                    bar.clear();
                    return;
                }
                pinyinSession.bindCandidateClicks(commitTarget, bar);
            }
        };

        onCommitTextListener = new OnCommitTextListener() {
            @Override
            public boolean onCommitText(Layout layout, String text) {
                if (layout == Layout.ZH_PINYIN && pinyinSession != null) {
                    pinyinSession.onCommitChar(text, bar);
                    refreshCandidates.run();
                    return true;
                }
                return false;
            }
        };

        onBackspaceListener = new OnBackspaceListener() {
            @Override
            public boolean onBackspace(Layout layout) {
                if (layout == Layout.ZH_PINYIN && pinyinSession != null) {
                    boolean consumed = pinyinSession.onBackspace(bar);
                    if (consumed) refreshCandidates.run();
                    return consumed;
                }
                return false;
            }
        };

        onSpaceListener = new OnSpaceListener() {
            @Override
            public boolean onSpace(Layout layout) {
                if (layout == Layout.ZH_PINYIN && pinyinSession != null) {
                    boolean consumed = pinyinSession.onSpaceCommitBest(commitTarget, bar);
                    if (consumed) refreshCandidates.run();
                    return consumed;
                }
                return false;
            }
        };

        final OnLayoutChangedListener existing = onLayoutChangedListener;
        onLayoutChangedListener = new OnLayoutChangedListener() {
            @Override
            public void onLayoutChanged(Layout layout) {
                if (existing != null) existing.onLayoutChanged(layout);
                if (layout != Layout.ZH_PINYIN && pinyinSession != null) {
                    pinyinSession.clear();
                    bar.clear();
                }
            }
        };
    }

    private void rebuild() {
        removeAllViews();
        if (DEBUG) Log.d(TAG, "rebuild layout=" + currentLayout + " view=" + System.identityHashCode(this));
        switch (currentLayout) {
            case EN:
                buildEnQwerty();
                break;
            case ZH_PINYIN:
                buildZhPinyin();
                break;
            case FR:
                buildFrAzerty();
                break;
            case AR:
                buildArabic();
                break;
            case SYMBOLS:
                buildSymbols();
                break;
            case NUMERIC:
                buildNumeric();
                break;
        }
    }

    private void buildEnQwerty() {
        buildFromAskXml("ask_layouts/en_qwerty.xml");
    }

    private void buildZhPinyin() {
        buildFromAskXml("ask_layouts/en_qwerty.xml");
    }

    private void buildFrAzerty() {
        buildFromAskXml("ask_layouts/fr_azerty.xml");
    }

    private void buildArabic() {
        buildFromAskXml("ask_layouts/ar_qwerty.xml");
    }

    private void buildSymbols() {
        addRow(list("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"));
        addRow(list("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"));
        addRow(list("abc", "_", "\"", "'", ":", ";", "!", "?", "⌫"));
        addRow(list("lang", "space", "enter"));
    }

    private void buildNumeric() {
        addRow(list("1", "2", "3"));
        addRow(list("4", "5", "6"));
        addRow(list("7", "8", "9"));
        addRow(list("123", "0", "⌫"));
        addRow(list("lang", "enter"));
    }

    private static List<String> list(String... items) {
        List<String> l = new ArrayList<>();
        for (String s : items) l.add(s);
        return l;
    }

    private void buildFromAskXml(String assetPath) {
        try {
            AskXmlKeyboardParser.Layout layout = AskXmlKeyboardParser.parseAsset(getContext().getAssets(), assetPath);

            int totalKeys = 0;
            for (List<AskXmlKeyboardParser.Key> row : layout.rows) {
                List<String> labels = new ArrayList<>();
                for (AskXmlKeyboardParser.Key key : row) {
                    String label = null;
                    if (key.code != null) {
                        if (key.code == -1) label = "⇧";
                        else if (key.code == -5) label = "⌫";
                        else label = key.label != null ? key.label : String.valueOf((char) (int) key.code);
                    } else if (key.label != null && !key.label.isEmpty()) {
                        label = key.label;
                    }
                    if (label != null) labels.add(label);
                }
                if (!labels.isEmpty()) {
                    totalKeys += labels.size();
                    addRow(labels);
                    if (DEBUG) Log.d(TAG, "addRow labels=" + labels.size() + " totalKeys=" + totalKeys);
                }
            }

            if (totalKeys == 0) {
                // Fallback: minimal qwerty when ASK parsing yields no keys.
                addRow(list("q","w","e","r","t","y","u","i","o","p"));
                addRow(list("a","s","d","f","g","h","j","k","l"));
                addRow(list("⇧","z","x","c","v","b","n","m","⌫"));
            }

            addRow(list("lang", "123", "space", "enter"));
            if (DEBUG) Log.d(TAG, "rebuild done childCount=" + getChildCount() + " view=" + System.identityHashCode(this));
            requestLayout();
            invalidate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse keyboard layout: " + assetPath, e);
        }
    }

    private void addRow(List<String> keys) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        int keyHeight = isLandscape ? dp(36) : dp(44);
        int fixedKeyWidth = isUltraWide ? dp(56) : 0;

        for (String label : keys) {
            Button btn = new Button(getContext());
            btn.setText(resolveKeyLabel(label));
            btn.setAllCaps(false);
            btn.setMinHeight(keyHeight);
            btn.setMinimumHeight(keyHeight);
            // Make keys visible against dark background
            btn.setBackgroundColor(Color.parseColor("#8E7CC3"));
            btn.setTextColor(Color.WHITE);

            if (isUltraWide) {
                int w = "space".equals(label) ? fixedKeyWidth * 4 : "enter".equals(label) || "lang".equals(label) ? fixedKeyWidth * 2 : fixedKeyWidth;
                LayoutParams lp = new LayoutParams(w, LayoutParams.WRAP_CONTENT);
                lp.setMarginStart(dp(2));
                lp.setMarginEnd(dp(2));
                btn.setLayoutParams(lp);
            } else {
                float weight = "space".equals(label) ? 3f : 1f;
                LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, weight);
                lp.setMarginStart(dp(2));
                lp.setMarginEnd(dp(2));
                btn.setLayoutParams(lp);
            }

            if ("⌫".equals(label)) {
                setupBackspaceRepeater(btn);
            } else {
                btn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onKey(label);
                    }
                });
            }

            row.addView(btn);
        }

        addView(row);
    }

    private String resolveKeyLabel(String label) {
        if ("space".equals(label)) return "Space";
        if ("enter".equals(label)) return "Enter";
        if ("abc".equals(label)) return "ABC";
        if ("123".equals(label)) return (currentLayout == Layout.NUMERIC) ? "#+=" : "123";
        if ("lang".equals(label)) {
            switch (currentLayout) {
                case EN: return "EN";
                case ZH_PINYIN: return "中";
                case FR: return "FR";
                case AR: return "AR";
                case SYMBOLS: return "#";
                case NUMERIC: return "123";
            }
        }
        return label;
    }

    @VisibleForTesting
    public void injectKey(String label) {
        onKey(label);
    }

    private void onKey(String label) {
        if (target == null) return;

        if ("⌫".equals(label)) {
            boolean consumed = onBackspaceListener != null && onBackspaceListener.onBackspace(currentLayout);
            if (!consumed) {
                int len = target.length();
                if (len > 0) target.delete(len - 1, len);
            }
        } else if ("enter".equals(label)) {
            target.append("\n");
        } else if ("space".equals(label)) {
            boolean consumed = onSpaceListener != null && onSpaceListener.onSpace(currentLayout);
            if (!consumed) target.append(" ");
        } else if ("⇧".equals(label)) {
            shift = !shift;
        } else if ("123".equals(label)) {
            setLayout(Layout.SYMBOLS);
        } else if ("abc".equals(label)) {
            if (inputMode == InputMode.NUMBER) setLayout(Layout.NUMERIC);
            else setLayout(Layout.EN);
        } else if ("lang".equals(label)) {
            if (inputMode == InputMode.NUMBER) {
                setLayout(Layout.NUMERIC);
                return;
            }
            if (inputMode == InputMode.PASSWORD) {
                setLayout(Layout.EN);
                return;
            }
            Layout next;
            switch (currentLayout) {
                case EN: next = Layout.ZH_PINYIN; break;
                case ZH_PINYIN: next = Layout.FR; break;
                case FR: next = Layout.AR; break;
                case AR:
                case SYMBOLS:
                case NUMERIC: next = Layout.EN; break;
                default: next = Layout.EN; break;
            }
            setLayout(next);
        } else {
            String ch = shift ? label.toUpperCase() : label;
            boolean consumed = onCommitTextListener != null && onCommitTextListener.onCommitText(currentLayout, ch);
            if (!consumed) target.append(ch);
            if (shift) shift = false;
        }
    }

    private void setupBackspaceRepeater(final Button btn) {
        final long initialDelayMs = 250L;
        final long repeatDelayMs = 50L;

        final boolean[] repeating = { false };
        final Runnable repeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!repeating[0]) return;
                onKey("⌫");
                handler.postDelayed(this, repeatDelayMs);
            }
        };

        btn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        onKey("⌫");
                        repeating[0] = true;
                        handler.postDelayed(repeatRunnable, initialDelayMs);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        repeating[0] = false;
                        handler.removeCallbacks(repeatRunnable);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
