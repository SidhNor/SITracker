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

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.andrada.sitracker.R;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;
import com.andrada.sitracker.ui.fragment.adapters.PublicationsPageAdapter;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.UIUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import de.greenrobot.event.EventBus;

@EActivity
@OptionsMenu(R.menu.main_menu)
public class SiMainActivity extends BaseActivity {


    @ViewById(R.id.details_pager)
    ViewPager pager;

    @ViewById(R.id.fragment_holder)
    View fragmentHolder;

    @Bean
    PublicationsPageAdapter pageAdapter;

    @InstanceState
    boolean pagerShown = false;
    @InstanceState
    long selectedId;

    private final ViewPager.OnPageChangeListener mPageListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            selectedId = pageAdapter.getItemDSForPosition(position).getId();
            getActionBarToolbar().setTitle(pageAdapter.getItemDSForPosition(position).getName());
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int priority = 1;
        EventBus.getDefault().register(this, priority);

        /*
        if (UIUtils.isTablet(this) && UIUtils.isLandscape(this)) {
            setContentView(R.layout.activity_si_main_two_pane);
        }
        */
        setContentView(R.layout.activity_si_main);
    }

    @AfterViews
    protected void afterViews() {
        mCurrentNavigationElement = (NavDrawerManager.NavDrawerItemAware) getFragmentManager().findFragmentByTag("currentFrag");
        if (mCurrentNavigationElement == null) {
            //Bootstrap app with initial fragment
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            AuthorsFragment authFrag = AuthorsFragment_.builder().build();
            mCurrentNavigationElement = authFrag;
            transaction.replace(R.id.fragment_holder, authFrag, "currentFrag");
            transaction.setCustomAnimations(0, 0);
            transaction.commit();
        }
        super.afterViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(AuthorSelectedEvent event) {
        shouldSkipOnePop = true;
        pager.setAdapter(pageAdapter);
        pager.setVisibility(View.VISIBLE);
        pagerShown = true;
        fragmentHolder.setVisibility(View.GONE);

        getDrawerManager().pushNavigationalState(event.authorName, false);

        selectedId = event.authorId;
        int positionToSelect = pageAdapter.getItemPositionForId(event.authorId);
        pager.setCurrentItem(positionToSelect);
        //getActionBarToolbar().setTitle(event.authorName);
        getActionBarUtil().autoShowOrHideActionBar(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (pagerShown) {
            shouldSkipOnePop = true;
            pager.setAdapter(pageAdapter);
            pager.setVisibility(View.VISIBLE);
            pagerShown = true;
            fragmentHolder.setVisibility(View.GONE);
            int positionToSelect = pageAdapter.getItemPositionForId(selectedId);
            pager.setCurrentItem(positionToSelect);
            getActionBarUtil().autoShowOrHideActionBar(true);
        }*/
        pager.setOnPageChangeListener(mPageListener);
        invalidateOptionsMenu();
        setContentTopClearance();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pager.setOnPageChangeListener(null);
    }

    public void setContentTopClearance() {
        if (mCurrentNavigationElement != null) {
            // configure fragment's top clearance to take our overlaid controls (Action Bar) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            setContentTopClearance(actionBarSize);
            mCurrentNavigationElement.setContentTopClearance(actionBarSize);
            pager.setPadding(pager.getPaddingLeft(), actionBarSize,
                    pager.getPaddingRight(), pager.getPaddingBottom());
            setProgressBarTopWhenActionBarShown(actionBarSize);
        }
    }

    @Override
    public void onBackPressed() {
        if (shouldSkipOnePop) {
            pager.setVisibility(View.GONE);
            pagerShown = false;
            fragmentHolder.setVisibility(View.VISIBLE);
        }
        super.onBackPressed();
    }
}
