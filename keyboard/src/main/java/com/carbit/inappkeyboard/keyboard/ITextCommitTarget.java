package com.carbit.inappkeyboard.keyboard;

/**
 * Keeps pinyin composing state + candidate list and commits to a target.
 * This is not a system IME. It is designed for in-app use.
 * Implementations can wrap EditText, AutoCompleteTextView, WebView, etc.
 */
public interface ITextCommitTarget {
    /** Insert text at current cursor (implementation decides exact placement). */
    void insert(String text);

    /** Delete the last {@code count} character(s) before cursor. */
    void deleteLastChar(int count);
}
