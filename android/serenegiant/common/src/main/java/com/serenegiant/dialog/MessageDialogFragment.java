package com.serenegiant.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * Compatibility Shim for MessageDialogFragment.
 */
public class MessageDialogFragment extends DialogFragment {
    public static MessageDialogFragment showDialog(final String title, final String message) {
        final MessageDialogFragment fragment = new MessageDialogFragment();
        return fragment;
    }
}
