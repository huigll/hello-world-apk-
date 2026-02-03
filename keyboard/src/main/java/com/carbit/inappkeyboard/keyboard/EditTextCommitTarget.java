package com.carbit.inappkeyboard.keyboard;

import android.widget.EditText;

/**
 * {@link ITextCommitTarget} that writes into an {@link EditText} (or {@link android.widget.AutoCompleteTextView}).
 */
public final class EditTextCommitTarget implements ITextCommitTarget {

    private final EditText editText;

    public EditTextCommitTarget(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void insert(String text) {
        if (editText == null || text == null) return;
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start < 0) start = editText.getText().length();
        if (end < 0) end = start;
        editText.getText().replace(Math.min(start, end), Math.max(start, end), text);
    }

    @Override
    public void deleteLastChar(int count) {
        if (editText == null || count <= 0) return;
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start < 0) start = editText.getText().length();
        if (end < 0) end = start;
        int from = Math.min(start, end);
        int to = Math.max(start, end);
        if (from > 0) {
            int deleteFrom = Math.max(0, from - count);
            editText.getText().delete(deleteFrom, to);
        }
    }
}
