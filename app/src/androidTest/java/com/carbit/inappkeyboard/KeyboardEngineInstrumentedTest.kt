package com.carbit.inappkeyboard

import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carbit.inappkeyboard.keyboard.InAppKeyboardPanelView
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardEngineInstrumentedTest {

    private fun assertLayoutCycle(keyboard: InAppKeyboardView, expected: List<InAppKeyboardView.Layout>) {
        expected.forEachIndexed { i, layout ->
            assertEquals("layout mismatch at step $i", layout, keyboard.currentLayout)
            if (i != expected.lastIndex) keyboard.injectKey("lang")
        }
    }

    @Test
    fun text_mode_injectKey_appends_text() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val et = a.findViewById<EditText>(R.id.et_text)

                et.setText("")
                panel.attachTo(et)

                keyboard.injectKey("a")
                keyboard.injectKey("b")
                keyboard.injectKey("c")

                assertEquals("abc", et.text.toString())
            }
        }
    }

    @Test
    fun text_mode_space_enter_backspace() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val et = a.findViewById<EditText>(R.id.et_text)

                et.setText("")
                panel.attachTo(et)

                keyboard.injectKey("a")
                keyboard.injectKey("space")
                keyboard.injectKey("b")
                keyboard.injectKey("enter")
                keyboard.injectKey("c")
                keyboard.injectKey("⌫")

                assertEquals("a b\n", et.text.toString())
            }
        }
    }

    @Test
    fun symbols_toggle_123_and_back_to_abc() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val et = a.findViewById<EditText>(R.id.et_text)

                et.setText("")
                panel.attachTo(et)

                // Enter symbols
                keyboard.injectKey("123")
                keyboard.injectKey("@")
                // Return to letters (symbols layout uses 'abc')
                keyboard.injectKey("abc")
                keyboard.injectKey("a")

                assertEquals("@a", et.text.toString())
            }
        }
    }

    @Test
    fun inputMode_auto_infers_layouts_from_inputType() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView

                // TEXT
                val etText = a.findViewById<EditText>(R.id.et_text)
                val textClass = etText.inputType and InputType.TYPE_MASK_CLASS
                assertEquals(InputType.TYPE_CLASS_TEXT, textClass)

                panel.attachTo(etText)
                assertEquals(InAppKeyboardView.InputMode.TEXT, keyboard.inputMode)

                // NUMBER
                val etNumber = a.findViewById<EditText>(R.id.et_number)
                panel.attachTo(etNumber)
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.currentLayout)

                // PHONE (treated as numeric)
                val etPhone = a.findViewById<EditText>(R.id.et_phone)
                panel.attachTo(etPhone)
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.currentLayout)

                // PASSWORD should land on EN.
                val etPassword = a.findViewById<EditText>(R.id.et_password)
                panel.attachTo(etPassword)
                assertEquals(InAppKeyboardView.Layout.EN, keyboard.currentLayout)
            }
        }
    }

    @Test
    fun text_mode_lang_cycles_EN_ZH_FR_AR_EN() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val et = a.findViewById<EditText>(R.id.et_text)

                panel.attachTo(et)
                keyboard.setLayout(InAppKeyboardView.Layout.EN)

                assertLayoutCycle(
                    keyboard,
                    listOf(
                        InAppKeyboardView.Layout.EN,
                        InAppKeyboardView.Layout.ZH_PINYIN,
                        InAppKeyboardView.Layout.FR,
                        InAppKeyboardView.Layout.AR,
                        InAppKeyboardView.Layout.EN,
                    ),
                )
            }
        }
    }

    @Test
    fun number_mode_symbols_toggle_returns_to_numeric() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val et = a.findViewById<EditText>(R.id.et_number)

                et.setText("")
                panel.attachTo(et)
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.currentLayout)

                // Toggle to symbols from numeric
                keyboard.injectKey("123")
                assertEquals(InAppKeyboardView.Layout.SYMBOLS, keyboard.currentLayout)

                // Note: number EditText will reject non-digit chars; use a digit that exists on symbols.
                keyboard.injectKey("1")

                // Back to numeric due to inputMode NUMBER
                keyboard.injectKey("abc")
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.currentLayout)
                keyboard.injectKey("2")

                assertEquals("12", et.text.toString())
            }
        }
    }

    @Test
    fun number_mode_injectKey_appends_digits() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val et = a.findViewById<EditText>(R.id.et_number)

                et.setText("")
                panel.attachTo(et)

                keyboard.injectKey("1")
                keyboard.injectKey("2")
                keyboard.injectKey("3")

                assertEquals("123", et.text.toString())
            }
        }
    }

    @Test
    fun password_mode_lang_key_does_not_cycle_and_no_candidates() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val candidateBar = panel.candidateBarView
                val et = a.findViewById<EditText>(R.id.et_password)

                et.setText("")
                panel.attachTo(et)

                // Try to change language
                val before = keyboard.currentLayout
                keyboard.injectKey("lang")
                val after = keyboard.currentLayout

                // Should stick to EN in password mode
                assertEquals(before, after)

                // Candidates should remain hidden
                assertFalse(candidateBar.visibility == View.VISIBLE)
            }
        }
    }

    @Test
    fun zh_pinyin_generates_candidates_and_click_commit() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                val panel = a.findViewById<InAppKeyboardPanelView>(R.id.keyboard_panel)
                val keyboard = panel.keyboardView
                val candidateBar = panel.candidateBarView
                val et = a.findViewById<EditText>(R.id.et_text)

                et.setText("")
                panel.attachTo(et)
                keyboard.setLayout(InAppKeyboardView.Layout.ZH_PINYIN)

                keyboard.injectKey("n")
                keyboard.injectKey("i")

                val container = candidateBar.findViewById<LinearLayout>(
                    com.carbit.inappkeyboard.keyboard.R.id.candidates_container
                )
                assertTrue("candidate bar should be visible", candidateBar.visibility == View.VISIBLE)
                assertTrue("expected >= 1 candidate", container.childCount >= 1)

                // Click first candidate should commit Chinese text into EditText.
                container.getChildAt(0).performClick()
                val s = et.text.toString()
                assertTrue("expected committed text after candidate click", s.isNotEmpty())
            }
        }
    }
}
