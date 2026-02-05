package com.carbit.inappkeyboard.keyboard;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PinyinDecoderInstrumentedTest {

    private PinyinDecoder createDecoder() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new PinyinDecoder(context);
    }

    @Test
    public void candidates_returns_list_and_respects_max() {
        PinyinDecoder decoder = createDecoder();
        List<String> list = decoder.candidates("ni", 5);
        assertNotNull(list);
        assertTrue(list.size() <= 5);
    }

    @Test
    public void choose_returns_non_empty_when_candidate_exists() {
        PinyinDecoder decoder = createDecoder();
        List<String> list = decoder.candidates("ni", 5);
        if (!list.isEmpty()) {
            String chosen = decoder.choose(0);
            assertNotNull(chosen);
            assertFalse(chosen.trim().isEmpty());
        }
    }

    @Test
    public void reset_allows_subsequent_search() {
        PinyinDecoder decoder = createDecoder();
        decoder.reset();
        List<String> list = decoder.candidates("hao", 5);
        assertNotNull(list);
    }
}
