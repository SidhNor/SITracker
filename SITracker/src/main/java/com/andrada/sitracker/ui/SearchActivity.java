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


import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.ui.fragment.RemoteAuthorsFragment;
import com.andrada.sitracker.ui.phone.MyAuthorsActivity_;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.UIUtils;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.LOGW;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

@SuppressLint("Registered")
@EActivity(R.layout.activity_search)
@OptionsMenu(R.menu.search_menu)
public class SearchActivity extends BaseActivity {

    private static final String TAG = makeLogTag(SearchActivity.class);

    @FragmentById(R.id.remote_authors_fragment)
    RemoteAuthorsFragment mAuthorsFragment;

    @ViewById(R.id.main_content)
    DrawShadowFrameLayout mDrawShadowFrameLayout;

    SearchView mSearchView = null;

    @InstanceState
    String mQuery = "";

    @InstanceState
    int mCurrentSearchType = 0;

    @AfterViews
    void afterViews() {
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setTitle(R.string.title_search);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpToFromChild(SearchActivity.this,
                        IntentCompat.makeMainActivity(new ComponentName(SearchActivity.this,
                                MyAuthorsActivity_.class)));
            }
        });


        String query = getIntent().getStringExtra(SearchManager.QUERY);
        if (query == null && mQuery != null) {
            query = mQuery;
        }
        mQuery = query;

        if (mSearchView != null) {
            mSearchView.setQuery(query, false);
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ListView collectionView = (ListView) findViewById(R.id.list);
        if (collectionView != null) {
            getActionBarUtil().enableActionBarAutoHide(collectionView);
        }
        getActionBarUtil().registerHideableHeaderView(findViewById(R.id.headerbar));
        populateSearchVariants();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        final MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            final SearchView view = (SearchView) MenuItemCompat.getActionView(searchItem);
            mSearchView = view;
            if (view == null) {
                LOGW(TAG, "Could not set up search view, view is null.");
            } else {
                view.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                view.setIconified(false);
                view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        view.clearFocus();
                        if (mAuthorsFragment != null) {
                            mAuthorsFragment.requestQueryUpdate(s, mCurrentSearchType);
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        mQuery = s;
                        return true;
                    }
                });
                view.setOnCloseListener(new SearchView.OnCloseListener() {
                    @Override
                    public boolean onClose() {
                        finish();
                        return false;
                    }
                });

                new ShowcaseView.Builder(this)
                        .setTarget(new ViewTarget(mSearchView))
                        .setContentTitle(getString(R.string.showcase_search_title))
                        .setContentText(getString(R.string.showcase_search_detail))
                        .setStyle(R.style.ShowcaseView_Base)
                        .singleShot(Constants.SHOWCASE_ADD_AUTHORS_SEARCH_SHOT_ID).build();

                if (!TextUtils.isEmpty(mQuery)) {
                    view.setQuery(mQuery, false);
                }
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuthorsFragment != null) {
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            int filterBarSize = getResources().getDimensionPixelSize(R.dimen.filterbar_height);
            mDrawShadowFrameLayout.setShadowTopOffset(actionBarSize + filterBarSize);
            mAuthorsFragment.setContentTopClearance(actionBarSize + filterBarSize
                    + getResources().getDimensionPixelSize(R.dimen.search_grid_padding));
        }
    }

    @Override
    public void actionBarVisibilityChanged(boolean shown) {
        super.actionBarVisibilityChanged(shown);
        //TODO Move this thing to base
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        LOGD(TAG, "SearchActivity.onNewIntent: " + intent);
        setIntent(intent);
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle args = intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, AppUriContract.buildSamlibSearchUri(query, 0)));
        if (mAuthorsFragment != null) {
            mAuthorsFragment.reloadFromArguments(args);
        }
    }

    private void populateSearchVariants() {
        Spinner searchOptionSpinner = (Spinner) findViewById(R.id.search_option_spinner);
        if (searchOptionSpinner != null) {
            List<String> items = new ArrayList<String>();
            items.add(getString(R.string.search_type_name));
            items.add(getString(R.string.search_type_keyword));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.search_spinner_item,
                    android.R.id.text1, items);
            adapter.setDropDownViewResource(R.layout.search_spinner_item_dropdown);
            searchOptionSpinner.setAdapter(adapter);
            searchOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    if (position >= 0 && position < 2) {
                        onSearchTypeSelected(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            if (mCurrentSearchType > 0) {
                searchOptionSpinner.setSelection(mCurrentSearchType);
            }
        }
    }

    private void onSearchTypeSelected(int position) {
        if (mCurrentSearchType == position) {
            return;
        }
        mCurrentSearchType = position;
        AnalyticsHelper.getInstance().sendEvent(
                Constants.GA_EXPLORE_CATEGORY,
                Constants.GA_EVENT_SEARCH_TYPE_CHANGED,
                Constants.GA_EVENT_SEARCH_TYPE_CHANGED);
        if (mAuthorsFragment != null && !TextUtils.isEmpty(mQuery)) {
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
            mAuthorsFragment.requestQueryUpdate(mQuery, mCurrentSearchType);
        }
    }
}
