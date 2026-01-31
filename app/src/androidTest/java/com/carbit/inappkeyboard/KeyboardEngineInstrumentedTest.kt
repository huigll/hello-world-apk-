package com.carbit.inappkeyboard

import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carbit.inappkeyboard.keyboard.CandidateBarView
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardEngineInstrumentedTest {

    @Test
    fun text_mode_injectKey_appends_text() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val keyboardContainer = a.findViewById<FrameLayout>(R.id.main_keyboard_container)
                val keyboard = keyboardContainer.getChildAt(0) as InAppKeyboardView
                val candidateBar = a.findViewById<CandidateBarView>(R.id.candidate_bar_view)
                val et = a.findViewById<EditText>(R.id.et_text)

                et.setText("")
                keyboard.attachTo(et, candidateBar)

                keyboard.injectKey("a")
                keyboard.injectKey("b")
                keyboard.injectKey("c")

                assertEquals("abc", et.text.toString())
            }
        }
    }

    @Test
    fun number_mode_injectKey_appends_digits() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val keyboardContainer = a.findViewById<FrameLayout>(R.id.main_keyboard_container)
                val keyboard = keyboardContainer.getChildAt(0) as InAppKeyboardView
                val candidateBar = a.findViewById<CandidateBarView>(R.id.candidate_bar_view)
                val et = a.findViewById<EditText>(R.id.et_number)

                et.setText("")
                keyboard.attachTo(et, candidateBar)

                keyboard.injectKey("1")
                keyboard.injectKey("2")
                keyboard.injectKey("3")

                assertEquals("123", et.text.toString())
            }
        }
    }

    @Test
    fun password_mode_injectKey_changes_value() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val keyboardContainer = a.findViewById<FrameLayout>(R.id.main_keyboard_container)
                val keyboard = keyboardContainer.getChildAt(0) as InAppKeyboardView
                val candidateBar = a.findViewById<CandidateBarView>(R.id.candidate_bar_view)
                val et = a.findViewById<EditText>(R.id.et_password)

                et.setText("")
                keyboard.attachTo(et, candidateBar)

                keyboard.injectKey("a")
                keyboard.injectKey("b")

                assertEquals("ab", et.text.toString())
            }
        }
    }

    @Test
    fun zh_pinyin_generates_candidates_after_typing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val keyboardContainer = a.findViewById<FrameLayout>(R.id.main_keyboard_container)
                val keyboard = keyboardContainer.getChildAt(0) as InAppKeyboardView
                val candidateBar = a.findViewById<CandidateBarView>(R.id.candidate_bar_view)
                val et = a.findViewById<EditText>(R.id.et_text)

                et.setText("")
                keyboard.attachTo(et, candidateBar)
                keyboard.setLayout(InAppKeyboardView.Layout.ZH_PINYIN)

                keyboard.injectKey("n")
                keyboard.injectKey("i")

                val container = candidateBar.findViewById<LinearLayout>(
                    com.carbit.inappkeyboard.keyboard.R.id.candidates_container
                )

                // Candidate visibility and count are the main stability target.
                assertTrue("candidate bar should be visible", candidateBar.visibility == android.view.View.VISIBLE)
                assertTrue("expected >= 1 candidate", container.childCount >= 1)
            }
        }
    }
}
