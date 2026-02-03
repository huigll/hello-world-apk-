package com.carbit.inappkeyboard;

import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import com.carbit.inappkeyboard.keyboard.ITextCommitTarget;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * {@link ITextCommitTarget} that injects text into the focused element of a WebView
 * (input, textarea, or contenteditable) via JavaScript.
 */
public final class WebViewCommitTarget implements ITextCommitTarget {

    private static final String TAG = "WebViewCommitTarget";

    private final WebView webView;

    public WebViewCommitTarget(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void insert(String text) {
        if (text == null || webView == null) return;
        String encoded = base64Encode(text);
        String js = "(function(){ try { var b = '" + encoded + "'; var t = decodeURIComponent(escape(atob(b))); " +
                "var el = document.activeElement; if (!el) return; " +
                "if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') { " +
                "var s = el.selectionStart, e = el.selectionEnd, v = el.value; " +
                "el.value = v.substring(0, s) + t + v.substring(e); " +
                "el.selectionStart = el.selectionEnd = s + t.length; " +
                "} else if (el.contentEditable === 'true') { document.execCommand('insertText', false, t); } " +
                "} catch (err) { console.log(err); } })();";
        evaluate(js);
    }

    @Override
    public void deleteLastChar(int count) {
        if (count <= 0 || webView == null) return;
        String js = "(function(){ try { " +
                "var el = document.activeElement; if (!el) return; var n = " + count + "; " +
                "if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') { " +
                "var s = el.selectionStart, e = el.selectionEnd, v = el.value; " +
                "if (s > 0) { var from = Math.max(0, s - n); " +
                "el.value = v.substring(0, from) + v.substring(e); " +
                "el.selectionStart = el.selectionEnd = from; } " +
                "} else if (el.contentEditable === 'true') { " +
                "for (var i = 0; i < n; i++) document.execCommand('backspace', false, null); } " +
                "} catch (err) { console.log(err); } })();";
        evaluate(js);
    }

    private void evaluate(String js) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null);
        } else {
            webView.loadUrl("javascript:" + js);
        }
    }

    private static String base64Encode(String text) {
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8.name());
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "UTF-8 not supported", e);
            return Base64.encodeToString(text.getBytes(), Base64.NO_WRAP);
        }
    }
}
