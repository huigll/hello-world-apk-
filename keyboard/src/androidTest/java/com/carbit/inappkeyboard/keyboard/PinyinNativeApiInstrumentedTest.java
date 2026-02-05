package com.carbit.inappkeyboard.keyboard;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PinyinNativeApiInstrumentedTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Force native library load.
        new PinyinDecoder(context);
    }

    @After
    public void tearDown() {
        try {
            PinyinDecoder.nativeImCloseDecoder();
        } catch (Throwable ignored) {
        }
    }

    @Test
    public void open_search_getChoice_choose_close_flow() throws Exception {
        assertTrue(openDecoder(context));

        PinyinDecoder.nativeImSetMaxLens(64, 64);
        PinyinDecoder.nativeImResetSearch();

        byte[] buf = buildPinyinBuffer("ni");
        int count = PinyinDecoder.nativeImSearch(buf, buf.length - 1);
        assertTrue(count >= 0);

        String choice0 = PinyinDecoder.nativeImGetChoice(0);
        assertNotNull(choice0);
        if (count > 0) {
            assertFalse(choice0.trim().isEmpty());
            int chooseRet = PinyinDecoder.nativeImChoose(0);
            assertTrue(chooseRet >= 0);
        }

        assertTrue(PinyinDecoder.nativeImCloseDecoder());
    }

    @Test
    public void reset_allows_multiple_searches() throws Exception {
        assertTrue(openDecoder(context));

        byte[] buf1 = buildPinyinBuffer("ni");
        int count1 = PinyinDecoder.nativeImSearch(buf1, buf1.length - 1);
        assertTrue(count1 >= 0);

        PinyinDecoder.nativeImResetSearch();

        byte[] buf2 = buildPinyinBuffer("hao");
        int count2 = PinyinDecoder.nativeImSearch(buf2, buf2.length - 1);
        assertTrue(count2 >= 0);
    }

    @Test
    public void setMaxLens_with_small_values_still_searches() throws Exception {
        assertTrue(openDecoder(context));

        PinyinDecoder.nativeImSetMaxLens(4, 4);
        byte[] buf = buildPinyinBuffer("ni");
        int count = PinyinDecoder.nativeImSearch(buf, buf.length - 1);
        assertTrue(count >= 0);
    }

    @Test
    public void search_empty_buffer_does_not_crash() throws Exception {
        assertTrue(openDecoder(context));

        byte[] buf = buildPinyinBuffer("");
        int count = PinyinDecoder.nativeImSearch(buf, 0);
        assertTrue(count >= 0);
    }

    @Test
    public void getChoice_out_of_range_returns_empty() throws Exception {
        assertTrue(openDecoder(context));

        byte[] buf = buildPinyinBuffer("ni");
        PinyinDecoder.nativeImSearch(buf, buf.length - 1);
        String choice = PinyinDecoder.nativeImGetChoice(9999);
        assertNotNull(choice);
        assertTrue(choice.trim().isEmpty());
    }

    @Test
    public void choose_within_range_returns_without_crash() throws Exception {
        assertTrue(openDecoder(context));

        byte[] buf = buildPinyinBuffer("ni");
        int count = PinyinDecoder.nativeImSearch(buf, buf.length - 1);
        if (count > 0) {
            // Return value may vary; just verify it does not throw.
            PinyinDecoder.nativeImChoose(0);
        }
    }

    @Test
    public void close_can_be_called_multiple_times() throws Exception {
        assertTrue(openDecoder(context));
        assertTrue(PinyinDecoder.nativeImCloseDecoder());
        assertTrue(PinyinDecoder.nativeImCloseDecoder());
    }

    @Test
    public void stress_random_pinyin_input_does_not_crash() {
        // Chinese mode stress: random 1-10 letters, ensure no native crash.
        PinyinDecoder decoder = new PinyinDecoder(context);
        PinyinImeSession session = new PinyinImeSession(decoder);
        ICandidateBar bar = new NoopCandidateBar();
        ITextCommitTarget target = new NoopCommitTarget();

        Random random = new Random(1337);
        for (int i = 0; i < 600; i++) {
            int len = 1 + random.nextInt(10);
            for (int j = 0; j < len; j++) {
                int op = random.nextInt(100);
                if (op < 70) {
                    char c = (char) ('a' + random.nextInt(26));
                    session.onCommitChar(String.valueOf(c), bar);
                } else if (op < 85) {
                    session.onBackspace(bar);
                } else {
                    session.clear();
                    bar.clear();
                }
            }
            // Occasionally commit best candidate and clear composing.
            if (random.nextInt(100) < 80) {
                session.onSpaceCommitBest(target, bar);
            } else {
                session.clear();
                bar.clear();
            }
        }
    }

    private static class NoopCandidateBar implements ICandidateBar {
        @Override
        public void setCandidates(java.util.List<String> candidates, OnCandidateClickListener onClick) {
            // no-op
        }

        @Override
        public void clear() {
            // no-op
        }
    }

    private static class NoopCommitTarget implements ITextCommitTarget {
        @Override
        public void insert(String text) {
            // no-op
        }

        @Override
        public void deleteLastChar(int count) {
            // no-op
        }
    }

    private boolean openDecoder(Context ctx) throws IOException {
        File usr = new File(ctx.getFilesDir(), "usr_dict.dat");
        if (!usr.exists()) {
            try {
                usr.createNewFile();
            } catch (IOException ignored) {
            }
        }

        try {
            AssetFileDescriptor afd = ctx.getResources().openRawResourceFd(R.raw.dict_pinyin);
            boolean ok = PinyinDecoder.nativeImOpenDecoderFd(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength(),
                    (usr.getAbsolutePath() + "\u0000").getBytes(StandardCharsets.UTF_8)
            );
            afd.close();
            return ok;
        } catch (RuntimeException e) {
            // Fallback when resource is compressed (openRawResourceFd throws).
            File dictFile = new File(ctx.getFilesDir(), "dict_pinyin.dat");
            if (!dictFile.exists() || dictFile.length() == 0) {
                try (java.io.InputStream input = ctx.getResources().openRawResource(R.raw.dict_pinyin);
                     java.io.OutputStream output = new java.io.FileOutputStream(dictFile)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = input.read(buf)) >= 0) {
                        output.write(buf, 0, n);
                    }
                }
            }
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(dictFile, ParcelFileDescriptor.MODE_READ_ONLY);
            boolean ok = PinyinDecoder.nativeImOpenDecoderFd(
                    pfd.getFileDescriptor(),
                    0L,
                    dictFile.length(),
                    (usr.getAbsolutePath() + "\u0000").getBytes(StandardCharsets.UTF_8)
            );
            pfd.close();
            return ok;
        }
    }

    private static byte[] buildPinyinBuffer(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, buf, 0, bytes.length);
        buf[bytes.length] = 0;
        return buf;
    }
}
