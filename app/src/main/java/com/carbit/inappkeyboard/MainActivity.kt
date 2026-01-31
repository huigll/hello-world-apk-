package com.carbit.inappkeyboard

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.carbit.inappkeyboard.keyboard.InAppKeyboardPanelView
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView

class MainActivity : AppCompatActivity() {

    private var activeEditText: EditText? = null
    private lateinit var keyboardPanel: InAppKeyboardPanelView
    private val keyboard: InAppKeyboardView
        get() = keyboardPanel.keyboardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvCurrent = findViewById<TextView>(R.id.tv_current)
        val btnText = findViewById<Button>(R.id.btn_text)
        val btnNumber = findViewById<Button>(R.id.btn_number)
        val btnPassword = findViewById<Button>(R.id.btn_password)
        val btnPhone = findViewById<Button>(R.id.btn_phone)
        val btnHide = findViewById<Button>(R.id.btn_hide)

        val etText = findViewById<EditText>(R.id.et_text)
        val etNumber = findViewById<EditText>(R.id.et_number)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etPhone = findViewById<EditText>(R.id.et_phone)

        keyboardPanel = findViewById(R.id.keyboard_panel)

        // Keep text direction aligned with selected layout (e.g. RTL for Arabic).
        keyboard.onLayoutChanged = { layout ->
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

        fun showField(type: String, et: EditText) {
            // Toggle visibility (keeps distinct EditTexts for each inputType)
            etText.visibility = if (et === etText) View.VISIBLE else View.GONE
            etNumber.visibility = if (et === etNumber) View.VISIBLE else View.GONE
            etPassword.visibility = if (et === etPassword) View.VISIBLE else View.GONE
            etPhone.visibility = if (et === etPhone) View.VISIBLE else View.GONE

            tvCurrent.text = "Current: $type"

            activeEditText = et
            et.requestFocus()

            keyboardPanel.attachTo(et)
        }

        fun bindEditText(et: EditText, label: String) {
            et.setOnClickListener {
                showField(label, et)
            }
            et.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showField(label, et)
                }
            }
        }

        bindEditText(etText, "Text")
        bindEditText(etNumber, "Number")
        bindEditText(etPassword, "Password")
        bindEditText(etPhone, "Phone")

        btnText.setOnClickListener { showField("Text", etText) }
        btnNumber.setOnClickListener { showField("Number", etNumber) }
        btnPassword.setOnClickListener { showField("Password", etPassword) }
        btnPhone.setOnClickListener { showField("Phone", etPhone) }
        btnHide.setOnClickListener {
            keyboardPanel.hideKeyboard()
            activeEditText?.clearFocus()
        }

        // Default to Text.
        showField("Text", etText)
    }

    override fun onDestroy() {
        try {
            keyboardPanel.release()
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }
}
