package com.carbit.inappkeyboard;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.carbit.inappkeyboard.keyboard.InAppKeyboardView;

public class VirtualScreenPresentation extends Presentation {

    public VirtualScreenPresentation(Context context, Display display) {
        super(context, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentation_virtual);

        final EditText editText = findViewById(R.id.et_input);
        FrameLayout keyboardContainer = findViewById(R.id.keyboard_container);

        try {
            editText.setShowSoftInputOnFocus(false);
        } catch (Throwable ignored) {
        }

        final InAppKeyboardView keyboard = new InAppKeyboardView(getContext());
        keyboard.setVisibility(View.GONE);
        keyboard.attachTarget(editText.getText());
        keyboard.setOnLayoutChangedListener(new InAppKeyboardView.OnLayoutChangedListener() {
            @Override
            public void onLayoutChanged(InAppKeyboardView.Layout layout) {
                if (layout == InAppKeyboardView.Layout.AR) {
                    editText.setTextDirection(View.TEXT_DIRECTION_RTL);
                    editText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                } else {
                    editText.setTextDirection(View.TEXT_DIRECTION_LTR);
                    editText.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                }
            }
        });
        keyboardContainer.addView(keyboard);

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                keyboard.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            }
        });
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyboard.setVisibility(View.VISIBLE);
            }
        });
    }
}
