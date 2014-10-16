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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.andrada.sitracker.BuildConfig;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.ui.fragment.RemoteAuthorsFragment;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;
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

    private ArrayList<String> mSearchTypes = new ArrayList<String>();

    @AfterViews
    void afterViews() {
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
            enableActionBarAutoHide(collectionView);
        }
        populateSearchVariants();
        registerHideableHeaderView(findViewById(R.id.headerbar));
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
    protected void onNewIntent(Intent intent) {
        LOGD(TAG, "SearchActivity.onNewIntent: " + intent);
        setIntent(intent);
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle args = intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, AppUriContract.buildSamlibSearchUri(query)));
        if (mAuthorsFragment != null) {
            mAuthorsFragment.reloadFromArguments(args);
        }
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
                            mAuthorsFragment.requestQueryUpdate(s);
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

                ShowcaseView.Builder bldr = new ShowcaseView.Builder(this)
                        .setTarget(new ViewTarget(mSearchView))
                        .setContentTitle(getString(R.string.showcase_search_title))
                        .setContentText(getString(R.string.showcase_search_detail))
                        .setStyle(R.style.ShowcaseView_Base);
                if (!BuildConfig.DEBUG) {
                    bldr.singleShot(Constants.SHOWCASE_ADD_AUTHORS_SEARCH_SHOT_ID);
                }
                bldr.build();

                if (!TextUtils.isEmpty(mQuery)) {
                    view.setQuery(mQuery, false);
                }
            }
        }
        return true;
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }

    private void populateSearchVariants() {
        Spinner searchOptionSpinner = (Spinner) findViewById(R.id.search_option_spinner);
        if (searchOptionSpinner != null) {
            mSearchTypes.clear();
            mSearchTypes.add(getString(R.string.search_type_name));
            mSearchTypes.add(getString(R.string.search_type_keyword));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.search_spinner_item,
                    mSearchTypes);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            searchOptionSpinner.setAdapter(adapter);
            searchOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    if (position >= 0 && position < mSearchTypes.size()) {
                        onSearchTypeSelected(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });
        }
    }

    private void onSearchTypeSelected(int position) {

    }
}
