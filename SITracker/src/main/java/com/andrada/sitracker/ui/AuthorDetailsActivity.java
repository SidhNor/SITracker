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

package com.andrada.sitracker.ui;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.fragment.PublicationsFragment_;
import com.andrada.sitracker.util.AnalyticsHelper;

import org.androidannotations.annotations.EActivity;

@SuppressLint("Registered")
@EActivity(R.layout.activity_author_details)
public class AuthorDetailsActivity extends BaseActivity {
    /*
        @ViewById(R.id.details_pager)
        ViewPager pager;

        @ViewById(R.id.tabs)
        TabLayout tabLayout;

    */
    //@InstanceState
    long selectedId;

    //@InstanceState
    String authorName;

    @Override
    public void onStart() {
        super.onStart();

        Uri authorUri = getIntent().getData();
        selectedId = AppUriContract.getAuthorId(authorUri);
        authorName = AppUriContract.getAuthorName(authorUri);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpToFromChild(AuthorDetailsActivity.this,
                        IntentCompat.makeMainActivity(new ComponentName(AuthorDetailsActivity.this,
                                SiMainActivity_.class)));
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Uri authorUri = getIntent().getData();
            selectedId = AppUriContract.getAuthorId(authorUri);
            authorName = AppUriContract.getAuthorName(authorUri);

            Fragment fragment = PublicationsFragment_.builder().activeAuthorId(selectedId).authorName(authorName).build();
            getFragmentManager().beginTransaction()
                    .add(R.id.root_container, fragment, "single_pane")
                    .commit();
        }

        AnalyticsHelper.getInstance().sendView(Constants.GA_SCREEN_AUTHOR_PUBLICATIONS);
    }

    /*
    private final ViewPager.OnPageChangeListener mPageListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            selectedId = pageAdapter.getItemDSForPosition(position).getId();
           // getActionBarToolbar().setTitle(pageAdapter.getItemDSForPosition(position).getName());
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    */

    @Override
    protected void onNewIntent(Intent intent) {
        Uri authorUri = intent.getData();
        setIntent(intent);

        if (authorUri == null) {
            return;
        }
        selectedId = AppUriContract.getAuthorId(authorUri);
        authorName = AppUriContract.getAuthorName(authorUri);
    }

    public void onEvent(AuthorSelectedEvent event) {
        /*pager.setAdapter(pageAdapter);

        selectedId = event.authorId;
        int positionToSelect = pageAdapter.getItemPositionForId(event.authorId);
        pager.setCurrentItem(positionToSelect);
        //getActionBarToolbar().setTitle(event.authorName);*/
    }


    @Override
    protected void onResume() {
        super.onResume();
        //pageAdapter.setListener(pageLoadListener);
        //pager.setAdapter(pageAdapter);
        //pager.addOnPageChangeListener(mPageListener);
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //pager.removeOnPageChangeListener(mPageListener);
    }
/*
    private PublicationsPageAdapter.PublicationsPageAdapterListener pageLoadListener = new PublicationsPageAdapter.PublicationsPageAdapterListener() {
        @Override
        public void pagesLoaded() {
            int positionToSelect = pageAdapter.getItemPositionForId(selectedId);
            pager.setCurrentItem(positionToSelect);
            tabLayout.setOnTabSelectedListener(null);
            tabLayout.setupWithViewPager(pager);
        }
    };
    */
}
