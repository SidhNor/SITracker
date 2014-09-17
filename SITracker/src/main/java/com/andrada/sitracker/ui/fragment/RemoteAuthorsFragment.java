package com.andrada.sitracker.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ViewStub;
import android.widget.ListView;

import com.andrada.sitracker.R;
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

@EFragment(R.layout.fragment_listview_with_empty)
public class RemoteAuthorsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<SearchedAuthor>> {

    private static final String TAG = makeLogTag(RemoteAuthorsFragment.class);

    @ViewById
    ListView list;
    @ViewById
    ViewStub empty;

    @Bean
    SearchResultsAdapter adapter;

    @AfterViews
    void bindAdapter() {
        list.setAdapter(adapter);
        empty.setLayoutResource(R.layout.empty_authors);
        list.setEmptyView(empty);
    }

    public void requestQueryUpdate(String query) {

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
            adapter.swapData(data);
            LOGD(TAG, "Data has " + data.size() + " items. Will now update collection view.");
        }
    }

    @Override
    public void onLoaderReset(Loader<List<SearchedAuthor>> listLoader) {

    }
}
