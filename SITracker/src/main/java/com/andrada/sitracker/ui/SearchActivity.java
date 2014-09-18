package com.andrada.sitracker.ui;


import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.ui.fragment.RemoteAuthorsFragment;
import com.andrada.sitracker.ui.fragment.RemoteAuthorsFragment_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsMenu;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.LOGW;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

@EActivity(R.layout.activity_search)
@OptionsMenu(R.menu.search_menu)
public class SearchActivity extends BaseActivity {

    private static final String TAG = makeLogTag(SearchActivity.class);

    RemoteAuthorsFragment mAuthorsFragment;

    SearchView mSearchView = null;
    String mQuery = "";

    @AfterViews
    void afterViews() {
        String query = getIntent().getStringExtra(SearchManager.QUERY);
        query = query == null ? "" : query;
        mQuery = query;

        FragmentManager fm = getSupportFragmentManager();
        mAuthorsFragment = (RemoteAuthorsFragment) fm.findFragmentById(R.id.fragment_container);

        if (mAuthorsFragment == null) {
            mAuthorsFragment = RemoteAuthorsFragment_.builder().build();
            /*
            Bundle args = intentToFragmentArguments(
                    new Intent(Intent.ACTION_VIEW, ScheduleContract.Sessions.buildSearchUri(query)));
            mAuthorsFragment.setArguments(args);*/
            fm.beginTransaction().add(R.id.fragment_container, mAuthorsFragment).commit();
        }

        if (mSearchView != null) {
            mSearchView.setQuery(query, false);
        }

        overridePendingTransition(0, 0);
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

                if (!TextUtils.isEmpty(mQuery)) {
                    view.setQuery(mQuery, false);
                }
            }
        }
        return true;
    }
}
