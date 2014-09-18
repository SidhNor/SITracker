package com.andrada.sitracker.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.loader.SamlibSearchLoader;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.fragment.adapters.SearchResultsAdapter;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import java.util.List;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

@EFragment(R.layout.fragment_search)
public class RemoteAuthorsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<SearchedAuthor>> {

    private static final String TAG = makeLogTag(RemoteAuthorsFragment.class);

    @ViewById
    ListView list;

    @ViewById
    ProgressBar loading;

    @ViewById
    TextView emptyText;

    private Bundle mArguments;
    private Uri mCurrentUri;

    @Bean
    SearchResultsAdapter adapter;

    @AfterViews
    void bindAdapter() {
        list.setAdapter(adapter);
        list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
    }

    public void requestQueryUpdate(String query) {
        reloadFromArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_SEARCH, AppUriContract.buildSamlibSearchUri(query))));
    }

    public void reloadFromArguments(Bundle arguments) {
        // Load new arguments
        if (arguments == null) {
            arguments = new Bundle();
        } else {
            // since we might make changes, don't meddle with caller's copy
            arguments = (Bundle) arguments.clone();
        }

        // save arguments so we can reuse it when reloading from content observer events
        mArguments = arguments;

        LOGD(TAG, "SessionsFragment reloading from arguments: " + arguments);
        mCurrentUri = arguments.getParcelable("_uri");
        LOGD(TAG, "SessionsFragment reloading, uri=" + mCurrentUri);
        reloadSearchData();
    }

    private void reloadSearchData() {
        LOGD(TAG, "Reloading search data");
        getLoaderManager().restartLoader(SamlibSearchLoader.SEARCH_TOKEN, mArguments, RemoteAuthorsFragment.this);
        emptyText.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
    }

    private void updateCollectionView(List<SearchedAuthor> data) {
        adapter.swapData(data);
        LOGD(TAG, "Data has " + data.size() + " items. Will now update collection view.");
        int itemCount = data.size();
        if (itemCount == 0) {
            showEmptyView();
        } else {
            hideEmptyView();
        }
    }

    private void hideEmptyView() {
        emptyText.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        final String searchQuery = AppUriContract.isSearchUri(mCurrentUri) ?
                AppUriContract.getSearchQuery(mCurrentUri) : null;

        if (AppUriContract.isSearchUri(mCurrentUri)
                && (TextUtils.isEmpty(searchQuery) || "*".equals(searchQuery))) {
            // Empty search query (for example, user hasn't started to type the query yet),
            // so don't show an empty view.
            emptyText.setText("");
            emptyText.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
        } else {
            // Showing authors as a result of search. If blank - show no resuls
            emptyText.setText(R.string.empty_search_results);
            emptyText.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<List<SearchedAuthor>> onCreateLoader(int id, Bundle data) {
        LOGD(TAG, "onCreateLoader, id=" + id + ", data=" + data);
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(data);
        Uri sessionsUri = intent.getData();
        Loader<List<SearchedAuthor>> loader = null;
        if (id == SamlibSearchLoader.SEARCH_TOKEN) {
            LOGD(TAG, "Creating search loader for " + sessionsUri);
            loader = new SamlibSearchLoader(getActivity(), sessionsUri);
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<List<SearchedAuthor>> loader, List<SearchedAuthor> data) {
        if (getActivity() == null) {
            return;
        }

        int token = loader.getId();
        LOGD(TAG, "Loader finished: search");
        if (token == SamlibSearchLoader.SEARCH_TOKEN) {
            updateCollectionView(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<SearchedAuthor>> listLoader) {

    }
}
