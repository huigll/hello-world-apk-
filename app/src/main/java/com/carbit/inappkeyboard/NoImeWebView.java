package com.carbit.inappkeyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

/**
 * WebView 不向系统提供 InputConnection，从而不弹出系统输入法。
 * 配合 in-app 键盘通过 JS 注入输入使用。
 */
public class NoImeWebView extends WebView {

    public NoImeWebView(Context context) {
        super(context);
    }

    public NoImeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoImeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return null;
    }
}
