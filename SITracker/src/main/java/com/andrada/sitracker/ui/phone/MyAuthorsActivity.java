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

package com.andrada.sitracker.ui.phone;

import android.app.Fragment;
import android.os.Bundle;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_authors_narrow)
@OptionsMenu(R.menu.main_menu)
public class MyAuthorsActivity extends BaseActivity {

    @ViewById(R.id.main_content)
    DrawShadowFrameLayout mDrawShadowFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        AuthorsFragment frag = (AuthorsFragment) getFragmentManager().findFragmentById(
                R.id.fragment_authors);
        enableActionBarAutoHide(frag.getListView());
        registerHideableHeaderView(findViewById(R.id.headerbar));
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Explore mode.
        return NAVDRAWER_ITEM_MY_AUTHORS;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();

        Fragment frag = getFragmentManager().findFragmentById(R.id.fragment_authors);
        if (frag != null) {
            // configure fragment's top clearance to take our overlaid controls (Action Bar) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            mDrawShadowFrameLayout.setShadowTopOffset(actionBarSize);
            ((AuthorsFragment) frag).setContentTopClearance(actionBarSize);
        }
    }
}
