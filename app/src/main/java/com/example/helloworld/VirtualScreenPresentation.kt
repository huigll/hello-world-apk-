package com.example.helloworld

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout

class VirtualScreenPresentation(context: Context, display: Display) : Presentation(context, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presentation_virtual)

        val editText = findViewById<EditText>(R.id.et_input)
        val keyboardContainer = findViewById<FrameLayout>(R.id.keyboard_container)

        // IMPORTANT: disable system IME in this context (especially for VirtualDisplay)
        // We will use our in-app keyboard view.
        try {
            editText.showSoftInputOnFocus = false
        } catch (_: Throwable) {
            // For very old APIs, ignore.
        }

        val keyboard = InAppKeyboardView(context).apply {
            visibility = View.GONE
            attachTarget(editText.text)
            onLayoutChanged = { layout ->
                // For Arabic we need RTL direction; others are LTR.
                if (layout == InAppKeyboardView.Layout.AR) {
                    editText.textDirection = View.TEXT_DIRECTION_RTL
                    editText.layoutDirection = View.LAYOUT_DIRECTION_RTL
                } else {
                    editText.textDirection = View.TEXT_DIRECTION_LTR
                    editText.layoutDirection = View.LAYOUT_DIRECTION_LTR
                }
            }
        }
        keyboardContainer.addView(keyboard)

        editText.setOnFocusChangeListener { _, hasFocus ->
            keyboard.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }
        editText.setOnClickListener {
            keyboard.visibility = View.VISIBLE
        }
    }
}
