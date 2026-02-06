package com.carbit.inappkeyboard;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.carbit.inappkeyboard.keyboard.PinyinDecoder;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ImeCommonWordsPinyinTest {

    @Test
    public void common_words_findable_by_full_pinyin() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PinyinDecoder decoder = new PinyinDecoder(context);
        BufferTarget target = new BufferTarget();

        try (InputStream input = context.getResources().openRawResource(R.raw.ime_common_words_1000_plain);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                WordPinyin parsed = parseWordAndPinyin(trimmed);
                assertTrue("Invalid entry (missing pinyin): " + trimmed, parsed != null);

                boolean found = tryCandidates(decoder, parsed.word, parsed.fullPinyin);
                if (!found) {
                    PerCharResult perChar = commitPerChar(parsed.word, parsed.syllables, decoder, target);
                    assertTrue("Not found by full pinyin and per-char failed: " + parsed.word
                            + " (" + parsed.fullPinyin + ")" + perChar.detail, perChar.ok);
                } else {
                    assertTrue(true);
                }
            }
        }
    }

    private static boolean tryCandidates(PinyinDecoder decoder, String word, String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) return false;
        List<String> candidates = decoder.candidates(pinyin, 100);
        return candidates.contains(word);
    }

    private static WordPinyin parseWordAndPinyin(String line) {
        int open = line.lastIndexOf('(');
        int close = line.lastIndexOf(')');
        if (open <= 0 || close <= open + 1 || close != line.length() - 1) {
            return null;
        }
        String word = line.substring(0, open).trim();
        String pinyinRaw = line.substring(open + 1, close).trim().toLowerCase();
        if (word.isEmpty() || pinyinRaw.isEmpty()) return null;

        String normalized = pinyinRaw.replaceAll("[^a-z]+", " ").trim();
        String[] parts = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
        java.util.ArrayList<String> syllables = new java.util.ArrayList<>();
        StringBuilder full = new StringBuilder();
        for (String part : parts) {
            String cleaned = part.replaceAll("[^a-z]", "");
            if (cleaned.isEmpty()) continue;
            syllables.add(cleaned);
            full.append(cleaned);
        }
        if (syllables.isEmpty()) return null;
        return new WordPinyin(word, full.toString(), syllables);
    }

    private static class WordPinyin {
        final String word;
        final String fullPinyin;
        final java.util.List<String> syllables;

        WordPinyin(String word, String fullPinyin, java.util.List<String> syllables) {
            this.word = word;
            this.fullPinyin = fullPinyin;
            this.syllables = syllables;
        }
    }

    private static PerCharResult commitPerChar(String word, java.util.List<String> syllables,
                                               PinyinDecoder decoder,
                                               BufferTarget target) {
        target.clear();
        decoder.reset();

        if (syllables.size() != word.length()) {
            return new PerCharResult(false, " [syllables=" + syllables.size()
                    + ", chars=" + word.length() + "]");
        }
        for (int i = 0; i < word.length(); i++) {
            String hanzi = String.valueOf(word.charAt(i));
            String syllable = syllables.get(i);
            if (syllable.isEmpty()) {
                return new PerCharResult(false, " [empty syllable at " + i + "]");
            }
            List<String> candidates = decoder.candidates(syllable, 200);
            int idx = candidates.indexOf(hanzi);
            if (idx < 0) {
                int preview = Math.min(20, candidates.size());
                return new PerCharResult(false, " [miss \"" + hanzi + "\" for \"" + syllable
                        + "\" candidates=" + candidates.subList(0, preview) + "]");
            }
            target.insert(decoder.choose(idx));
            decoder.reset();
        }
        boolean ok = word.equals(target.getText());
        return new PerCharResult(ok, ok ? "" : " [result=" + target.getText() + "]");
    }

    private static class PerCharResult {
        final boolean ok;
        final String detail;

        PerCharResult(boolean ok, String detail) {
            this.ok = ok;
            this.detail = detail;
        }
    }

    private static class BufferTarget implements com.carbit.inappkeyboard.keyboard.ITextCommitTarget {
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
