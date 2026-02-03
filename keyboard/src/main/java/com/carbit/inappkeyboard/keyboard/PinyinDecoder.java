package com.carbit.inappkeyboard.keyboard;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Very small wrapper around AOSP PinyinIME native decoder.
 * We use it ONLY to get candidate strings for a pinyin buffer.
 */
public class PinyinDecoder implements IPinyinDecoder {

    private final Context context;
    private boolean inited = false;

    static {
        try {
            System.loadLibrary("jni_pinyinime");
        } catch (Throwable t) {
            Log.e("PinyinDecoder", "Failed to load native library jni_pinyinime", t);
        }
    }

    public static native boolean nativeImOpenDecoderFd(
            java.io.FileDescriptor fd,
            long startOffset,
            long length,
            byte[] usrDictPathBytes
    );

    public static native void nativeImSetMaxLens(int maxSpsLen, int maxHzsLen);

    public static native boolean nativeImCloseDecoder();

    public static native void nativeImResetSearch();

    public static native int nativeImSearch(byte[] pyBuf, int pyLen);

    public static native String nativeImGetChoice(int choiceId);

    public static native int nativeImChoose(int choiceId);

    public PinyinDecoder(Context context) {
        this.context = context.getApplicationContext();
    }

    public void initIfNeeded() {
        if (inited) return;

        File usr = new File(context.getFilesDir(), "usr_dict.dat");
        if (!usr.exists()) {
            try {
                usr.createNewFile();
            } catch (IOException ignored) {
            }
        }

        boolean ok = false;
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.dict_pinyin);
            ok = nativeImOpenDecoderFd(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength(),
                    (usr.getAbsolutePath() + "\u0000").getBytes(StandardCharsets.UTF_8)
            );
            afd.close();
        } catch (Throwable t) {
            Log.w("PinyinDecoder", "openRawResourceFd failed (likely compressed). Falling back to extracted file.", t);

            File dictFile = new File(context.getFilesDir(), "dict_pinyin.dat");
            if (!dictFile.exists() || dictFile.length() == 0) {
                try {
                    android.content.res.Resources res = context.getResources();
                    java.io.InputStream input = res.openRawResource(R.raw.dict_pinyin);
                    java.io.OutputStream output = new java.io.FileOutputStream(dictFile);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = input.read(buf)) >= 0) {
                        output.write(buf, 0, n);
                    }
                    input.close();
                    output.close();
                } catch (Throwable e) {
                    Log.e("PinyinDecoder", "Failed to extract dict", e);
                }
            }

            try {
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(dictFile, ParcelFileDescriptor.MODE_READ_ONLY);
                ok = nativeImOpenDecoderFd(
                        pfd.getFileDescriptor(),
                        0L,
                        dictFile.length(),
                        (usr.getAbsolutePath() + "\u0000").getBytes(StandardCharsets.UTF_8)
                );
                pfd.close();
            } catch (Throwable e) {
                Log.e("PinyinDecoder", "Failed to open extracted dict", e);
            }
        }

        if (ok) {
            nativeImSetMaxLens(64, 64);
            inited = true;
        } else {
            Log.e("PinyinDecoder", "Failed to initialize pinyin decoder");
        }
    }

    @Override
    public void reset() {
        if (!inited) return;
        nativeImResetSearch();
    }

    @Override
    public List<String> candidates(String pinyin, int max) {
        initIfNeeded();
        if (!inited) return new ArrayList<>();

        byte[] bytes = pinyin.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, buf, 0, bytes.length);
        buf[bytes.length] = 0;
        nativeImSearch(buf, bytes.length);

        List<String> out = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            String c = nativeImGetChoice(i);
            if (c == null || c.trim().isEmpty()) break;
            out.add(c);
        }
        return out;
    }

    @Override
    public String choose(int index) {
        initIfNeeded();
        if (!inited) return "";

        String chosen = nativeImGetChoice(index);
        nativeImChoose(index);
        return chosen;
    }

    public void close() {
        if (!inited) return;
        nativeImCloseDecoder();
        inited = false;
    }
}
