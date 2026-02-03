package com.carbit.inappkeyboard.keyboard;

/**
 * Keeps pinyin composing state + candidate list and commits to a target.
 * This is not a system IME. It is designed for in-app use.
 */
public interface ITextCommitTarget {
    /** Insert text at current cursor (implementation decides exact placement). */
    void insert(String text);
}
