package com.andrada.sitracker.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

/**
 * Created by ggodonoga on 27/05/13.
 */
public class ClipboardHelper {

    public static CharSequence getClipboardText(Context context) {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence clipboardChars;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            clipboardChars = getClipboardHoneycomb(clipboard);
        } else {
            clipboardChars = getClipboardFroyo(clipboard);
        }
        return clipboardChars;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static CharSequence getClipboardHoneycomb(ClipboardManager clipboard) {
        if (clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText();
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private static CharSequence getClipboardFroyo(ClipboardManager clipboard) {
        //noinspection deprecation,AndroidLintNewApi
        return clipboard.getText();
    }
}
