package com.andrada.sitracker.util;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.os.Build;

/**
 * Created by ggodonoga on 27/05/13.
 */
public class ClipboardHelper {

    public static CharSequence getClipboardText(Context context) {

        CharSequence clipboardChars;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            android.content.ClipboardManager clipboard =  (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardChars = getClipboardHoneycomb(clipboard);
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardChars = getClipboardFroyo(clipboard);
        }
        return clipboardChars;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static CharSequence getClipboardHoneycomb(android.content.ClipboardManager clipboard) {
        if (clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText();
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private static CharSequence getClipboardFroyo(android.text.ClipboardManager clipboard) {
        //noinspection deprecation,AndroidLintNewApi
        return clipboard.getText();
    }
}
