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

import android.os.Bundle;

import com.andrada.sitracker.R;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsMenu;

@EActivity(R.layout.activity_si_main)
@OptionsMenu(R.menu.main_menu)
public class SiMainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        goToNavDrawerItem(NavDrawerManager.NAVDRAWER_ITEM_MY_AUTHORS);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mCurrentNavigationElement != null) {
            getActionBarUtil().enableActionBarAutoHide(mCurrentNavigationElement.getRecyclerView());
            getActionBarUtil().registerHideableHeaderView(findViewById(R.id.headerbar));
        }
    }

    @Override
    public int getSelfNavDrawerItem() {
        if (mCurrentNavigationElement != null) {
            return mCurrentNavigationElement.getSelfNavDrawerItem();
        }
        return NavDrawerManager.NAVDRAWER_ITEM_INVALID;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        setTopClearance();
    }

    private void setTopClearance() {
        if (mCurrentNavigationElement != null) {
            // configure fragment's top clearance to take our overlaid controls (Action Bar) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            mCurrentNavigationElement.setContentTopClearance(actionBarSize);
            setProgressBarTopWhenActionBarShown(actionBarSize);
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        if (mCurrentNavigationElement != null) {
            return mCurrentNavigationElement.canCollectionViewScrollUp();
        }
        return super.canSwipeRefreshChildScrollUp();
    }
}
