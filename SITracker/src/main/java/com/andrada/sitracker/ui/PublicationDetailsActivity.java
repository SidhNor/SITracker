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

package com.andrada.sitracker.ui;

import android.app.Fragment;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.fragment.PublicationInfoFragment_;
import com.andrada.sitracker.util.BeamUtils;

import de.keyboardsurfer.android.widget.crouton.Crouton;

public class PublicationDetailsActivity extends SimpleSinglePaneActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //UIUtils.tryTranslateHttpIntent(this);
        BeamUtils.tryUpdateIntentFromBeam(this);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        if (shouldBeFloatingWindow()) {
            setupFloatingWindow();
        }
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Uri publicationUri = getIntent().getData();
            BeamUtils.setBeamPublicationUri(this, publicationUri);
        }

        setTitle("");
    }

    @Override
    protected Fragment onCreatePane() {
        return PublicationInfoFragment_.builder().build();
    }

    private void setupFloatingWindow() {
        // configure this Activity as a floating window, dimming the background
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.publication_details_floating_width);
        params.height = getResources().getDimensionPixelSize(R.dimen.publication_details_floating_height);
        params.alpha = 1;
        params.dimAmount = 0.7f;
        params.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    private boolean shouldBeFloatingWindow() {
        Resources.Theme theme = getTheme();
        TypedValue floatingWindowFlag = new TypedValue();
        if (theme == null || !theme.resolveAttribute(R.attr.isFloatingWindow, floatingWindowFlag, true)) {
            // isFloatingWindow flag is not defined in theme
            return false;
        }
        return (floatingWindowFlag.data != 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }
}
