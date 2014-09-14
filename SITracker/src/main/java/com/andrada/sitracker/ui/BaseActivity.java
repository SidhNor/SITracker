/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui;


import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.andrada.sitracker.R;
import com.andrada.sitracker.util.UIUtils;
import com.google.android.gms.analytics.GoogleAnalytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.andrada.sitracker.util.LogUtils.makeLogTag;

/**
 * A base activity that handles common functionality in the app.
 */
public abstract class BaseActivity extends ActionBarActivity {

    private static final String TAG = makeLogTag(BaseActivity.class);

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    @NotNull
    protected static Bundle intentToFragmentArguments(@Nullable Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(extras);
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    @NotNull
    public static Intent fragmentArgumentsToIntent(@Nullable Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    protected void setHasTabs() {
        if (!UIUtils.isTablet(this)
                && getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE) {
            // Only show the tab bar's shadow
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.actionbar_background_noshadow));
        }
    }
}
