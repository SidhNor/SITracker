/*
 * Copyright 2014 Gleb Godonoga.
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

package com.andrada.sitracker.util;


import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LUtils {

    private static Typeface sMediumTypeface;

    protected ActionBarActivity mActivity;

    private LUtils(ActionBarActivity activity) {
        mActivity = activity;
    }

    public static LUtils getInstance(ActionBarActivity activity) {
        return new LUtils(activity);
    }

    private static boolean hasL() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public void startActivityWithTransition(Intent intent, final View clickedView,
                                            final String transitionName) {
        ActivityOptionsCompat options = null;
        if (clickedView != null && !TextUtils.isEmpty(transitionName)) {
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    mActivity, clickedView, transitionName);
        }
        ActivityCompat.startActivity(mActivity, intent, (options != null) ? options.toBundle() : null);
    }

    public void setMediumTypeface(TextView textView) {
        if (hasL()) {
            if (sMediumTypeface == null) {
                sMediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            }

            textView.setTypeface(sMediumTypeface);
        } else {
            textView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        }
    }

    public int getStatusBarColor() {
        if (!hasL()) {
            // On pre-L devices, you can have any status bar color so long as it's black.
            return Color.BLACK;
        }

        return mActivity.getWindow().getStatusBarColor();
    }

    public void setStatusBarColor(int color) {
        if (!hasL()) {
            return;
        }

        mActivity.getWindow().setStatusBarColor(color);
    }
}
