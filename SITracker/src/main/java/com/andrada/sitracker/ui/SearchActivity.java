package com.andrada.sitracker.ui;


import android.app.SearchManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SearchView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.fragment.RemoteAuthorsFragment;
import com.andrada.sitracker.ui.fragment.RemoteAuthorsFragment_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsMenu;

@EActivity(R.layout.activity_search)
@OptionsMenu(R.menu.search_menu)
public class SearchActivity extends BaseActivity {

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
}
