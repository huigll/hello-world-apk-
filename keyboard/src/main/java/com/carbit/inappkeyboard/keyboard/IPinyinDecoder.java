package com.carbit.inappkeyboard.keyboard;

import java.util.List;

/**
 * Very small wrapper around AOSP PinyinIME native decoder.
 * We use it ONLY to get candidate strings for a pinyin buffer.
 */
public interface IPinyinDecoder {
    void reset();

    List<String> candidates(String pinyin, int max);

    String choose(int index);
}
