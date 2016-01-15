/*
 * Copyright 2016 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.components.AboutDialogView;
import com.andrada.sitracker.ui.components.AboutDialogView_;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;
import android.webkit.WebView;

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

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.action_about)
                .customView(aboutBodyView, true)
                .positiveText(android.R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static void showWhatsNew(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(WhatsNewDialog.FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new WhatsNewDialog().show(ft, WhatsNewDialog.FRAGMENT_TAG);
    }

    public static void showOpenSourceLicenses(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
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
            return new MaterialDialog.Builder(getActivity())
                    .title(R.string.whats_new)
                    .customView(webView, true)
                    .positiveText(android.R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
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

            return new MaterialDialog.Builder(getActivity())
                    .title(R.string.about_licenses)
                    .customView(webView, false)
                    .positiveText(android.R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }
}
