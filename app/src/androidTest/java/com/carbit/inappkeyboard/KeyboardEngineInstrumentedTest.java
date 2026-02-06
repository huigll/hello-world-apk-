package com.carbit.inappkeyboard;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.carbit.inappkeyboard.keyboard.CandidateBarView;
import com.carbit.inappkeyboard.keyboard.InAppKeyboardPanelView;
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class KeyboardEngineInstrumentedTest {

    private void assertLayoutCycle(InAppKeyboardView keyboard, List<InAppKeyboardView.Layout> expected) {
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("layout mismatch at step " + i, expected.get(i), keyboard.getCurrentLayout());
            if (i != expected.size() - 1) keyboard.injectKey("lang");
        }
    }

    @Test
    public void text_mode_injectKey_appends_text() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                EditText et = activity.findViewById(R.id.et_text);

                et.setText("");
                panel.attachTo(et);

                keyboard.injectKey("a");
                keyboard.injectKey("b");
                keyboard.injectKey("c");

                assertEquals("abc", et.getText().toString());
            });
        }
    }

    @Test
    public void text_mode_space_enter_backspace() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                EditText et = activity.findViewById(R.id.et_text);

                et.setText("");
                panel.attachTo(et);

                keyboard.injectKey("a");
                keyboard.injectKey("space");
                keyboard.injectKey("b");
                keyboard.injectKey("enter");
                keyboard.injectKey("c");
                keyboard.injectKey("⌫");

                assertEquals("a b\n", et.getText().toString());
            });
        }
    }

    @Test
    public void symbols_toggle_123_and_back_to_abc() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                EditText et = activity.findViewById(R.id.et_text);

                et.setText("");
                panel.attachTo(et);

                keyboard.injectKey("123");
                keyboard.injectKey("@");
                keyboard.injectKey("abc");
                keyboard.injectKey("a");

                assertEquals("@a", et.getText().toString());
            });
        }
    }

    @Test
    public void inputMode_auto_infers_layouts_from_inputType() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();

                EditText etText = activity.findViewById(R.id.et_text);
                int textClass = etText.getInputType() & InputType.TYPE_MASK_CLASS;
                assertEquals(InputType.TYPE_CLASS_TEXT, textClass);

                // Ensure auto inference is enabled for this test.
                keyboard.setInputMode(InAppKeyboardView.InputMode.AUTO);
                panel.attachTo(etText);
                assertEquals(InAppKeyboardView.InputMode.TEXT, keyboard.getInputMode());

                EditText etNumber = activity.findViewById(R.id.et_number);
                keyboard.setInputMode(InAppKeyboardView.InputMode.AUTO);
                panel.attachTo(etNumber);
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.getCurrentLayout());

                EditText etPhone = activity.findViewById(R.id.et_phone);
                keyboard.setInputMode(InAppKeyboardView.InputMode.AUTO);
                panel.attachTo(etPhone);
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.getCurrentLayout());

                EditText etPassword = activity.findViewById(R.id.et_password);
                keyboard.setInputMode(InAppKeyboardView.InputMode.AUTO);
                panel.attachTo(etPassword);
                assertEquals(InAppKeyboardView.Layout.EN, keyboard.getCurrentLayout());
            });
        }
    }

    @Test
    public void text_mode_lang_cycles_EN_ZH_FR_AR_EN() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                EditText et = activity.findViewById(R.id.et_text);

                panel.attachTo(et);
                keyboard.setInputMode(InAppKeyboardView.InputMode.TEXT);
                keyboard.setLayout(InAppKeyboardView.Layout.EN);

                assertLayoutCycle(keyboard, Arrays.asList(
                        InAppKeyboardView.Layout.EN,
                        InAppKeyboardView.Layout.ZH_PINYIN,
                        InAppKeyboardView.Layout.FR,
                        InAppKeyboardView.Layout.AR,
                        InAppKeyboardView.Layout.EN
                ));
            });
        }
    }

    @Test
    public void number_mode_symbols_toggle_returns_to_numeric() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                EditText et = activity.findViewById(R.id.et_number);

                et.setText("");
                keyboard.setInputMode(InAppKeyboardView.InputMode.AUTO);
                panel.attachTo(et);
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.getCurrentLayout());

                keyboard.injectKey("123");
                assertEquals(InAppKeyboardView.Layout.SYMBOLS, keyboard.getCurrentLayout());

                keyboard.injectKey("1");
                keyboard.injectKey("abc");
                assertEquals(InAppKeyboardView.Layout.NUMERIC, keyboard.getCurrentLayout());
                keyboard.injectKey("2");

                assertEquals("12", et.getText().toString());
            });
        }
    }

    @Test
    public void number_mode_injectKey_appends_digits() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                EditText et = activity.findViewById(R.id.et_number);

                et.setText("");
                keyboard.setInputMode(InAppKeyboardView.InputMode.AUTO);
                panel.attachTo(et);

                keyboard.injectKey("1");
                keyboard.injectKey("2");
                keyboard.injectKey("3");

                assertEquals("123", et.getText().toString());
            });
        }
    }

    @Test
    public void password_mode_lang_key_does_not_cycle_and_no_candidates() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                CandidateBarView candidateBar = panel.getCandidateBarView();
                EditText et = activity.findViewById(R.id.et_password);

                et.setText("");
                keyboard.setInputMode(InAppKeyboardView.InputMode.AUTO);
                panel.attachTo(et);

                InAppKeyboardView.Layout before = keyboard.getCurrentLayout();
                keyboard.injectKey("lang");
                InAppKeyboardView.Layout after = keyboard.getCurrentLayout();

                assertEquals(before, after);
                assertFalse(candidateBar.getVisibility() == View.VISIBLE);
            });
        }
    }

    @Test
    public void zh_pinyin_generates_candidates_and_click_commit() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                InAppKeyboardPanelView panel = activity.findViewById(R.id.keyboard_panel);
                InAppKeyboardView keyboard = panel.getKeyboardView();
                CandidateBarView candidateBar = panel.getCandidateBarView();
                EditText et = activity.findViewById(R.id.et_text);

                et.setText("");
                panel.attachTo(et);
                keyboard.setLayout(InAppKeyboardView.Layout.ZH_PINYIN);

                keyboard.injectKey("n");
                keyboard.injectKey("i");

                LinearLayout container = candidateBar.findViewById(com.carbit.inappkeyboard.keyboard.R.id.candidates_container);
                assertTrue("candidate bar should be visible", candidateBar.getVisibility() == View.VISIBLE);
                assertTrue("expected >= 1 candidate", container.getChildCount() >= 1);

                container.getChildAt(0).performClick();
                String s = et.getText().toString();
                assertTrue("expected committed text after candidate click", s != null && !s.isEmpty());
            });
        }
    }
}
