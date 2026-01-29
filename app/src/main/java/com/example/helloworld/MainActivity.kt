package com.example.helloworld

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editText = findViewById<EditText>(R.id.et_main_input)
        val keyboardContainer = findViewById<FrameLayout>(R.id.main_keyboard_container)

        // Disable system IME and use our in-app keyboard view.
        try {
            editText.showSoftInputOnFocus = false
        } catch (_: Throwable) {
            // ignore on older API
        }

        val keyboard = InAppKeyboardView(this).apply {
            visibility = View.GONE
            attachTarget(editText.text)
            onLayoutChanged = { layout ->
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
