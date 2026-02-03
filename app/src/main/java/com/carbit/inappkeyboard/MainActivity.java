package com.carbit.inappkeyboard;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.carbit.inappkeyboard.keyboard.InAppKeyboardPanelView;
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    private View activeInputView;
    private InAppKeyboardPanelView keyboardPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvCurrent = findViewById(R.id.tv_current);
        Button btnText = findViewById(R.id.btn_text);
        Button btnNumber = findViewById(R.id.btn_number);
        Button btnPassword = findViewById(R.id.btn_password);
        Button btnPhone = findViewById(R.id.btn_phone);
        Button btnAutoComplete = findViewById(R.id.btn_autocomplete);
        Button btnWebView = findViewById(R.id.btn_webview);
        Button btnHide = findViewById(R.id.btn_hide);

        final EditText etText = findViewById(R.id.et_text);
        final EditText etNumber = findViewById(R.id.et_number);
        final EditText etPassword = findViewById(R.id.et_password);
        final EditText etPhone = findViewById(R.id.et_phone);
        final AutoCompleteTextView actvAutoComplete = findViewById(R.id.actv_autocomplete);
        final WebView webViewInput = findViewById(R.id.webview_input);

        keyboardPanel = findViewById(R.id.keyboard_panel);
        final InAppKeyboardView keyboard = keyboardPanel.getKeyboardView();

        keyboardPanel.bindTo(etText);
        keyboardPanel.bindTo(etNumber);
        keyboardPanel.bindTo(etPassword);
        keyboardPanel.bindTo(etPhone);
        keyboardPanel.bindTo(actvAutoComplete);

        keyboard.setOnLayoutChangedListener(new InAppKeyboardView.OnLayoutChangedListener() {
            @Override
            public void onLayoutChanged(InAppKeyboardView.Layout layout) {
                View v = activeInputView;
                if (v instanceof EditText) {
                    EditText et = (EditText) v;
                    if (layout == InAppKeyboardView.Layout.AR) {
                        et.setTextDirection(View.TEXT_DIRECTION_RTL);
                        et.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    } else {
                        et.setTextDirection(View.TEXT_DIRECTION_LTR);
                        et.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    }
                }
            }
        });

        View.OnClickListener showText = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("Text", etText, tvCurrent, etText, etNumber, etPassword, etPhone, actvAutoComplete, webViewInput);
            }
        };
        View.OnClickListener showNumber = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("Number", etNumber, tvCurrent, etText, etNumber, etPassword, etPhone, actvAutoComplete, webViewInput);
            }
        };
        View.OnClickListener showPassword = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("Password", etPassword, tvCurrent, etText, etNumber, etPassword, etPhone, actvAutoComplete, webViewInput);
            }
        };
        View.OnClickListener showPhone = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("Phone", etPhone, tvCurrent, etText, etNumber, etPassword, etPhone, actvAutoComplete, webViewInput);
            }
        };
        View.OnClickListener showAutoComplete = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("AutoComplete", actvAutoComplete, tvCurrent, etText, etNumber, etPassword, etPhone, actvAutoComplete, webViewInput);
            }
        };
        View.OnClickListener showWebView = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("WebView", webViewInput, tvCurrent, etText, etNumber, etPassword, etPhone, actvAutoComplete, webViewInput);
            }
        };

        btnText.setOnClickListener(showText);
        btnNumber.setOnClickListener(showNumber);
        btnPassword.setOnClickListener(showPassword);
        btnPhone.setOnClickListener(showPhone);
        btnAutoComplete.setOnClickListener(showAutoComplete);
        btnWebView.setOnClickListener(showWebView);
        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyboardPanel.hide();
                if (activeInputView != null && activeInputView instanceof EditText) {
                    ((EditText) activeInputView).clearFocus();
                }
            }
        });

        setupWebView(webViewInput);
        showField("Text", etText, tvCurrent, etText, etNumber, etPassword, etPhone, actvAutoComplete, webViewInput);
    }

    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:8px;background:#1a1a1a;color:#eee'>" +
                "<p style='color:#888'>Tap the input below, then use the in-app keyboard:</p>" +
                "<input type='text' id='inp' placeholder='WebView input' style='width:100%;padding:12px;font-size:16px;color:#fff;background:#333;border:1px solid #555' />" +
                "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        webView.requestFocus(View.FOCUS_DOWN);
    }

    private void showField(String type, View inputView,
                          TextView tvCurrent,
                          EditText etText, EditText etNumber, EditText etPassword, EditText etPhone,
                          AutoCompleteTextView actvAutoComplete, WebView webViewInput) {
        etText.setVisibility(inputView == etText ? View.VISIBLE : View.GONE);
        etNumber.setVisibility(inputView == etNumber ? View.VISIBLE : View.GONE);
        etPassword.setVisibility(inputView == etPassword ? View.VISIBLE : View.GONE);
        etPhone.setVisibility(inputView == etPhone ? View.VISIBLE : View.GONE);
        actvAutoComplete.setVisibility(inputView == actvAutoComplete ? View.VISIBLE : View.GONE);
        webViewInput.setVisibility(inputView == webViewInput ? View.VISIBLE : View.GONE);

        tvCurrent.setText("Current: " + type);

        activeInputView = inputView;
        Log.d(TAG, "Showing keyboard for " + type);

        if (inputView instanceof EditText) {
            keyboardPanel.attachTo((EditText) inputView);
            keyboardPanel.show();
        } else if (inputView instanceof WebView) {
            keyboardPanel.attachTo(new WebViewCommitTarget((WebView) inputView));
            keyboardPanel.show();
        }

        InAppKeyboardView keyboard = keyboardPanel.getKeyboardView();
        if ("Number".equals(type) || "Phone".equals(type)) {
            keyboard.setInputMode(InAppKeyboardView.InputMode.NUMBER);
            keyboard.setLayout(InAppKeyboardView.Layout.NUMERIC);
        } else if ("Password".equals(type)) {
            keyboard.setInputMode(InAppKeyboardView.InputMode.PASSWORD);
            keyboard.setLayout(InAppKeyboardView.Layout.EN);
        } else {
            keyboard.setInputMode(InAppKeyboardView.InputMode.TEXT);
            keyboard.setLayout(InAppKeyboardView.Layout.EN);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            keyboardPanel.release();
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }
}
