package com.andrada.sitracker.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;
import android.webkit.WebView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.components.AboutDialogView;
import com.andrada.sitracker.ui.components.AboutDialogView_;

public class AboutDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "about_dialog";

    private static final String VERSION_UNAVAILABLE = "N/A";

    public AboutDialog() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        PackageManager pm = getActivity().getPackageManager();
        String packageName = getActivity().getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        SpannableStringBuilder aboutBody = new SpannableStringBuilder();
        SpannableString licensesLink = new SpannableString(getString(R.string.about_licenses));
        licensesLink.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                showOpenSourceLicenses(getActivity());
            }
        }, 0, licensesLink.length(), 0);
        SpannableString whatsNewLink = new SpannableString(getString(R.string.whats_new));
        whatsNewLink.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                showWhatsNew(getActivity());
            }
        }, 0, whatsNewLink.length(), 0);
        aboutBody.append(licensesLink);
        aboutBody.append("\n\n");
        aboutBody.append(whatsNewLink);

        AboutDialogView aboutBodyView = AboutDialogView_.build(getActivity());
        aboutBodyView.bindData(getString(R.string.app_version_format, versionName), aboutBody);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_about)
                .setView(aboutBodyView)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                ).create();
    }

    public static void showWhatsNew(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(WhatsNewDialog.FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new WhatsNewDialog().show(ft, WhatsNewDialog.FRAGMENT_TAG);
    }

    public static void showOpenSourceLicenses(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(OpenSourceLicensesDialog.FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new OpenSourceLicensesDialog().show(ft, OpenSourceLicensesDialog.FRAGMENT_TAG);
    }

    public static class WhatsNewDialog extends DialogFragment {
        public static final String FRAGMENT_TAG = "dialog_whatsnew";

        public WhatsNewDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = new WebView(getActivity());
            webView.loadData(getString(R.string.change_log), "text/html; charset=utf-8", "utf-8");
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.whats_new)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }

    public static class OpenSourceLicensesDialog extends DialogFragment {
        public static final String FRAGMENT_TAG = "dialog_licenses";

        public OpenSourceLicensesDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.about_licenses)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }
}
