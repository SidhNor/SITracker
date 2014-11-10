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

package com.andrada.sitracker.ui.tablet;

import android.os.Bundle;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.PublicationsFragment;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_authors_wide)
public class MyAuthorsMultipaneActivity extends BaseActivity {

    @ViewById(R.id.main_content)
    DrawShadowFrameLayout mDrawShadowFrameLayout;

    @FragmentById(R.id.fragment_publications)
    PublicationsFragment mPubFragment;
    @FragmentById(R.id.fragment_authors)
    AuthorsFragment mAuthorsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Ignore animations
        overridePendingTransition(0, 0);
    }

    @AfterViews
    void afterViews() {
        mDrawShadowFrameLayout.setShadowTopOffset(UIUtils.calculateActionBarSize(this));
    }

    @Override
    public int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Explore mode.
        return NavDrawerManager.NAVDRAWER_ITEM_MY_AUTHORS;
    }
}
