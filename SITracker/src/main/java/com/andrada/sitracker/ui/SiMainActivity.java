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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

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
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;

@EActivity
@OptionsMenu(R.menu.main_menu)
public class SiMainActivity extends BaseActivity {


    @ViewById(R.id.details_pager)
    ViewPager pager;

    @ViewById(R.id.fragment_holder)
    View fragmentHolder;

    @Bean
    PublicationsPageAdapter pageAdapter;

    public static final String AUTHORS_PROCESSED_EXTRA = "authors_total_processed";
    public static final String AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA = "authors_successfully_imported";
    @Extra(AUTHORS_PROCESSED_EXTRA)
    int authorsProcessed = -1;
    @Extra(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA)
    int authorsSuccessfullyImported = -1;

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
        attemptToShowImportProgress();
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

    @Override
    protected void onNewIntent(@NotNull Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(AUTHORS_PROCESSED_EXTRA)) {
                authorsProcessed = extras.getInt(AUTHORS_PROCESSED_EXTRA);
            }
            if (extras.containsKey(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA)) {
                authorsSuccessfullyImported = extras.getInt(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA);
            }
        }
    }


    private void attemptToShowImportProgress() {
        if (authorsProcessed != -1 && authorsSuccessfullyImported != -1) {
            View view = getLayoutInflater().inflate(R.layout.crouton_import_result, null);
            TextView totalTextV = (TextView) view.findViewById(R.id.totalAuthorsText);
            totalTextV.setText(getResources().getString(R.string.author_import_total_crouton_message,
                    authorsProcessed));
            TextView successTextV = (TextView) view.findViewById(R.id.successAuthorsText);
            successTextV.setText(getResources().getString(R.string.author_import_processed_crouton_message,
                    authorsSuccessfullyImported));
            TextView failedTextV = (TextView) view.findViewById(R.id.failedAuthorsText);
            failedTextV.setText(getResources().getString(R.string.author_import_failed_crouton_message,
                    authorsProcessed - authorsSuccessfullyImported));
            Configuration croutonConfiguration = new Configuration.Builder()
                    .setDuration(Configuration.DURATION_LONG).build();
            Crouton mNoNetworkCrouton = Crouton.make(this, view);
            mNoNetworkCrouton.setConfiguration(croutonConfiguration);
            mNoNetworkCrouton.show();

            //Remove extras to avoid reinitialization on config change
            getIntent().removeExtra(AUTHORS_PROCESSED_EXTRA);
            getIntent().removeExtra(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA);
            authorsSuccessfullyImported = -1;
            authorsProcessed = -1;
        }
    }
}
