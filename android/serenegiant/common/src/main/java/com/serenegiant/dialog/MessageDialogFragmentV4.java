package com.serenegiant.dialog;

import androidx.fragment.app.DialogFragment;

/**
 * Compatibility Shim for MessageDialogFragmentV4.
 */
public class MessageDialogFragmentV4 extends DialogFragment {
    public static MessageDialogFragmentV4 showDialog(final String title, final String message) {
        return new MessageDialogFragmentV4();
    }
}
