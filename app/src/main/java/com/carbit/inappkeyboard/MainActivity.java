package com.carbit.inappkeyboard;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.carbit.inappkeyboard.keyboard.InAppKeyboardPanelView;
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    private EditText activeEditText;
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
        Button btnHide = findViewById(R.id.btn_hide);

        final EditText etText = findViewById(R.id.et_text);
        final EditText etNumber = findViewById(R.id.et_number);
        final EditText etPassword = findViewById(R.id.et_password);
        final EditText etPhone = findViewById(R.id.et_phone);

        keyboardPanel = findViewById(R.id.keyboard_panel);
        final InAppKeyboardView keyboard = keyboardPanel.getKeyboardView();

        keyboard.setOnLayoutChangedListener(new InAppKeyboardView.OnLayoutChangedListener() {
            @Override
            public void onLayoutChanged(InAppKeyboardView.Layout layout) {
                EditText et = activeEditText;
                if (et != null) {
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
                showField("Text", etText, tvCurrent, etText, etNumber, etPassword, etPhone);
            }
        };
        View.OnClickListener showNumber = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("Number", etNumber, tvCurrent, etText, etNumber, etPassword, etPhone);
            }
        };
        View.OnClickListener showPassword = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("Password", etPassword, tvCurrent, etText, etNumber, etPassword, etPhone);
            }
        };
        View.OnClickListener showPhone = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showField("Phone", etPhone, tvCurrent, etText, etNumber, etPassword, etPhone);
            }
        };

        btnText.setOnClickListener(showText);
        btnNumber.setOnClickListener(showNumber);
        btnPassword.setOnClickListener(showPassword);
        btnPhone.setOnClickListener(showPhone);
        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyboardPanel.hide();
                if (activeEditText != null) activeEditText.clearFocus();
            }
        });

        showField("Text", etText, tvCurrent, etText, etNumber, etPassword, etPhone);
    }

    private void showField(String type, EditText et,
                           TextView tvCurrent,
                           EditText etText, EditText etNumber, EditText etPassword, EditText etPhone) {
        etText.setVisibility(et == etText ? View.VISIBLE : View.GONE);
        etNumber.setVisibility(et == etNumber ? View.VISIBLE : View.GONE);
        etPassword.setVisibility(et == etPassword ? View.VISIBLE : View.GONE);
        etPhone.setVisibility(et == etPhone ? View.VISIBLE : View.GONE);

        tvCurrent.setText("Current: " + type);

        activeEditText = et;
        Log.d(TAG, "Showing keyboard for " + type);
        keyboardPanel.bindTo(et);

        // Explicitly align layout by input type.
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
