package com.carbit.inappkeyboard

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.carbit.inappkeyboard.keyboard.CandidateBarView
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView

class MainActivity : AppCompatActivity() {

    private var activeEditText: EditText? = null
    private lateinit var keyboard: InAppKeyboardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etText = findViewById<EditText>(R.id.et_text)
        val etNumber = findViewById<EditText>(R.id.et_number)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etPhone = findViewById<EditText>(R.id.et_phone)

        val keyboardContainer = findViewById<FrameLayout>(R.id.main_keyboard_container)
        val candidateBar = findViewById<CandidateBarView>(R.id.candidate_bar_view)

        keyboard = InAppKeyboardView(this).apply {
            visibility = View.GONE

            // Keep text direction aligned with selected layout (e.g. RTL for Arabic).
            onLayoutChanged = { layout ->
                val et = activeEditText
                if (et != null) {
                    if (layout == InAppKeyboardView.Layout.AR) {
                        et.textDirection = View.TEXT_DIRECTION_RTL
                        et.layoutDirection = View.LAYOUT_DIRECTION_RTL
                    } else {
                        et.textDirection = View.TEXT_DIRECTION_LTR
                        et.layoutDirection = View.LAYOUT_DIRECTION_LTR
                    }
                }
            }
        }
        keyboardContainer.addView(keyboard)

        fun bindEditText(et: EditText) {
            et.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) return@setOnFocusChangeListener

                activeEditText = et

                // Only show candidate bar for normal text fields.
                // (Password/number/phone will be inferred by library and candidates will be disabled.)
                keyboard.attachTo(et, candidateBar)
                keyboard.visibility = View.VISIBLE
            }

            et.setOnClickListener {
                activeEditText = et
                keyboard.attachTo(et, candidateBar)
                keyboard.visibility = View.VISIBLE
            }
        }

        bindEditText(etText)
        bindEditText(etNumber)
        bindEditText(etPassword)
        bindEditText(etPhone)

        // Focus the first field by default.
        etText.requestFocus()
    }

    override fun onDestroy() {
        try {
            keyboard.release()
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }
}
