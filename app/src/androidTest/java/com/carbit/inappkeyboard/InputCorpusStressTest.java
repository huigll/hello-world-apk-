package com.carbit.inappkeyboard;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.carbit.inappkeyboard.keyboard.ICandidateBar;
import com.carbit.inappkeyboard.keyboard.ITextCommitTarget;
import com.carbit.inappkeyboard.keyboard.PinyinDecoder;
import com.carbit.inappkeyboard.keyboard.PinyinImeSession;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class InputCorpusStressTest {

    @Test
    public void zhwiki_titles_can_be_entered_via_ime_api() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PinyinDecoder decoder = new PinyinDecoder(context);
        PinyinImeSession session = new PinyinImeSession(decoder);
        CaptureCandidateBar bar = new CaptureCandidateBar();
        BufferTarget target = new BufferTarget();

        try (InputStream input = context.getResources().openRawResource(R.raw.zhwiki_latest_all_titles_in_ns0);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { // skip header
                    first = false;
                    continue;
                }

                target.clear();
                session.clear();
                bar.clear();

                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    if (ch >= 'a' && ch <= 'z') {
                        session.onCommitChar(String.valueOf(ch), bar);
                        // Avoid overlong composing strings to prevent native crashes.
                        if (session.composingText().length() >= 10) {
                            commitRawIfNeeded(session, bar, target);
                        }
                    } else {
                        commitRawIfNeeded(session, bar, target);
                        target.insert(String.valueOf(ch));
                    }
                }
                commitRawIfNeeded(session, bar, target);

                assertEquals(line, target.getText());
            }
        }
    }

    private static void commitRawIfNeeded(PinyinImeSession session, CaptureCandidateBar bar, BufferTarget target) {
        if (!session.hasComposing()) return;
        session.bindCandidateClicks(target, bar);
        if (bar.lastOnClick != null && !bar.lastCandidates.isEmpty()) {
            // index 0 is raw pinyin letters
            bar.lastOnClick.onClick(0, bar.lastCandidates.get(0));
        }
    }

    private static class CaptureCandidateBar implements ICandidateBar {
        List<String> lastCandidates = Collections.emptyList();
        OnCandidateClickListener lastOnClick = null;

        @Override
        public void setCandidates(List<String> candidates, OnCandidateClickListener onClick) {
            lastCandidates = candidates != null ? candidates : Collections.<String>emptyList();
            lastOnClick = onClick;
        }

        @Override
        public void clear() {
            lastCandidates = Collections.emptyList();
            lastOnClick = null;
        }
    }

    private static class BufferTarget implements ITextCommitTarget {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public void insert(String text) {
            sb.append(text);
        }

        @Override
        public void deleteLastChar(int count) {
            if (count <= 0) return;
            int len = sb.length();
            if (len == 0) return;
            sb.delete(Math.max(0, len - count), len);
        }

        void clear() {
            sb.setLength(0);
        }

        String getText() {
            return sb.toString();
        }
    }
}
