package com.example.helloworld

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var pinyin: PinyinDecoder
    private val composing = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pinyin = PinyinDecoder(this)

        val editText = findViewById<EditText>(R.id.et_main_input)
        val keyboardContainer = findViewById<FrameLayout>(R.id.main_keyboard_container)
        val candidatesContainer = findViewById<LinearLayout>(R.id.candidates_container)

        // Disable system IME and use our in-app keyboard view.
        try {
            editText.showSoftInputOnFocus = false
        } catch (_: Throwable) {
            // ignore on older API
        }

        fun refreshCandidates() {
            candidatesContainer.removeAllViews()
            if (composing.isEmpty()) return

            val list = pinyin.candidates(composing.toString(), max = 10)
            list.forEachIndexed { index, cand ->
                val btn = Button(this).apply {
                    text = cand
                    isAllCaps = false
                    setOnClickListener {
                        // choose & commit
                        val commit = pinyin.choose(index)
                        editText.text.insert(editText.selectionStart.coerceAtLeast(editText.text.length), commit)
                        composing.setLength(0)
                        pinyin.reset()
                        refreshCandidates()
                    }
                }
                candidatesContainer.addView(btn)
            }
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

                // Clear pinyin state when leaving ZH mode.
                if (layout != InAppKeyboardView.Layout.ZH_PINYIN) {
                    composing.setLength(0)
                    pinyin.reset()
                    refreshCandidates()
                }
            }

            // Intercept key presses for ZH mode to build pinyin buffer.
            onCommitText = { layout, text ->
                if (layout == InAppKeyboardView.Layout.ZH_PINYIN) {
                    composing.append(text)
                    refreshCandidates()
                    true
                } else {
                    false
                }
            }

            onBackspace = { layout ->
                if (layout == InAppKeyboardView.Layout.ZH_PINYIN && composing.isNotEmpty()) {
                    composing.setLength(composing.length - 1)
                    refreshCandidates()
                    true
                } else {
                    false
                }
            }

            onSpace = { layout ->
                if (layout == InAppKeyboardView.Layout.ZH_PINYIN && composing.isNotEmpty()) {
                    // commit best candidate
                    val list = pinyin.candidates(composing.toString(), max = 1)
                    val commit = list.firstOrNull() ?: composing.toString()
                    editText.text.insert(editText.selectionStart.coerceAtLeast(editText.text.length), commit)
                    composing.setLength(0)
                    pinyin.reset()
                    refreshCandidates()
                    true
                } else {
                    false
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

    override fun onDestroy() {
        try {
            pinyin.close()
        } catch (_: Throwable) {}
        super.onDestroy()
    }
}
