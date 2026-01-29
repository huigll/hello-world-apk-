package com.example.helloworld

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.keyboard.CandidateBarView
import com.example.keyboard.InAppKeyboardView
import com.example.keyboard.PinyinDecoder
import com.example.keyboard.PinyinImeSession

class MainActivity : AppCompatActivity() {

    private lateinit var pinyin: PinyinDecoder
    private lateinit var pinyinSession: PinyinImeSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pinyin = PinyinDecoder(this)
        pinyinSession = PinyinImeSession(pinyin)

        val editText = findViewById<EditText>(R.id.et_main_input)
        val keyboardContainer = findViewById<FrameLayout>(R.id.main_keyboard_container)
        val candidateBar = findViewById<CandidateBarView>(R.id.candidate_bar_view)

        // Disable system IME and use our in-app keyboard view.
        try {
            editText.showSoftInputOnFocus = false
        } catch (_: Throwable) {
            // ignore on older API
        }

        fun refreshCandidates() {
            if (!pinyinSession.hasComposing()) {
                candidateBar.clear()
                return
            }
            // Build candidates and wire click-to-commit.
            pinyinSession.bindCandidateClicks(editText, candidateBar)
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
                    pinyinSession.clear()
                    refreshCandidates()
                }
            }

            // Intercept key presses for ZH mode to build pinyin buffer.
            onCommitText = { layout, text ->
                if (layout == InAppKeyboardView.Layout.ZH_PINYIN) {
                    pinyinSession.onCommitChar(text, candidateBar)
                    refreshCandidates()
                    true
                } else {
                    false
                }
            }

            onBackspace = { layout ->
                if (layout == InAppKeyboardView.Layout.ZH_PINYIN) {
                    val consumed = pinyinSession.onBackspace(candidateBar)
                    if (consumed) refreshCandidates()
                    consumed
                } else {
                    false
                }
            }

            onSpace = { layout ->
                if (layout == InAppKeyboardView.Layout.ZH_PINYIN) {
                    val consumed = pinyinSession.onSpaceCommitBest(editText, candidateBar)
                    if (consumed) refreshCandidates()
                    consumed
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
