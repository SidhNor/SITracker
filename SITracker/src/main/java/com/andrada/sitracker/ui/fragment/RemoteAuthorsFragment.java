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

package com.andrada.sitracker.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.events.AuthorAddedEvent;
import com.andrada.sitracker.exceptions.SearchException;
import com.andrada.sitracker.loader.AsyncTaskResult;
import com.andrada.sitracker.loader.SamlibSearchLoader;
import com.andrada.sitracker.tasks.AddAuthorTask;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.components.CollectionView;
import com.andrada.sitracker.ui.fragment.adapters.SearchResultsAdapter;
import com.andrada.sitracker.util.AnalyticsHelper;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;


@EFragment(R.layout.fragment_search)
public class RemoteAuthorsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<AsyncTaskResult<List<SearchedAuthor>>>, SearchResultsAdapter.Callbacks {

    private static final String TAG = makeLogTag(RemoteAuthorsFragment.class);
    private static SearchResultsAdapter.Callbacks sDummyCallbacks = new SearchResultsAdapter.Callbacks() {
        @Override
        public void onAuthorSelected(SearchedAuthor author) {
        }
    };
    @ViewById
    CollectionView list;
    @ViewById
    ProgressBar loading;
    @ViewById
    TextView emptyText;
    @Bean
    SearchResultsAdapter adapter;
    private Bundle mArguments;
    private Uri mCurrentUri;

    public void onEvent(@NotNull AuthorAddedEvent event) {
        //Cancel any further delivery
        EventBus.getDefault().cancelEventDelivery(event);
        if (event.authorUrl != null) {
            //Find author with this url
            SearchedAuthor auth = adapter.getItemById(event.authorUrl);
            if (auth != null) {
                auth.setAdded(true);
                ((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
            }
        }
        loading.setVisibility(View.GONE);
        String message = event.message;

        AnalyticsHelper.getInstance().sendEvent(
                Constants.GA_EXPLORE_CATEGORY,
                Constants.GA_EVENT_AUTHOR_ADDED,
                Constants.GA_EVENT_AUTHOR_ADDED);

        if (message.length() != 0) {
            Style.Builder alertStyle = new Style.Builder()
                    .setTextAppearance(android.R.attr.textAppearanceLarge)
                    .setPaddingInPixels(25)
                    .setBackgroundColorValue(Style.holoRedLight);
            Crouton.makeText(getActivity(), message, alertStyle.build()).show();
        }
    }

    @Override
    public void onAuthorSelected(SearchedAuthor author) {
        final SearchedAuthor authorToAdd = author;
        if (author.isAdded()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.add_author_confirmation))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //TODO make sure to run this in UI thread
                        loading.setVisibility(View.VISIBLE);
                        new AddAuthorTask(getActivity()).execute(authorToAdd.getAuthorUrl());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    @UiThread(delay = 100)
    void requestUpdateCollectionView(List<SearchedAuthor> data) {
        updateCollectionView(data);
    }

    public void requestQueryUpdate(String query) {
        //Test query for URL
        if (query.matches(Constants.SIMPLE_URL_REGEX) && query.startsWith(Constants.HTTP_PROTOCOL)) {
            //This looks like an url
            loading.setVisibility(View.VISIBLE);
            new AddAuthorTask(getActivity()).execute(query);
        } else {
            reloadFromArguments(BaseActivity.intentToFragmentArguments(
                    new Intent(Intent.ACTION_SEARCH, AppUriContract.buildSamlibSearchUri(query))));
        }
    }

    public void setContentTopClearance(int topClearance) {
        list.setContentTopClearance(topClearance);
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
        list.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
    }

    private void updateCollectionView(List<SearchedAuthor> data) {
        adapter.swapData(data);
        LOGD(TAG, "Data has " + data.size() + " items. Will now update collection view.");
        int itemCount = data.size();
        CollectionView.Inventory inv;
        if (itemCount == 0) {
            showEmptyView();
            inv = new CollectionView.Inventory();
        } else {
            hideEmptyView();
            inv = prepareInventory();
        }
        list.setCollectionAdapter(adapter);
        list.updateInventory(inv, true);

    }

    private CollectionView.Inventory prepareInventory() {
        LOGD(TAG, "Preparing collection view inventory.");
        final int displayCols = getResources().getInteger(R.integer.search_grid_columns);
        CollectionView.InventoryGroup group = new CollectionView.InventoryGroup(1000)
                .setDisplayCols(displayCols)
                .setShowHeader(false);
        int dataIndex = -1;
        while ((dataIndex + 1) < adapter.getCount()) {
            ++dataIndex;
            group.addItemWithCustomDataIndex(dataIndex);
        }
        CollectionView.Inventory inventory = new CollectionView.Inventory();
        inventory.addGroup(group);
        return inventory;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (adapter != null) {
            adapter.setCallbacks(this);
            if (adapter.getCount() > 0) {
                requestUpdateCollectionView(adapter.getData());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setRetainInstance(true);
        adapter.setCallbacks(this);
        //noinspection VariableNotUsedInsideIf
        if (mCurrentUri != null) {
            // Only if this is a config change should we initLoader(), to reconnect with an
            // existing loader. Otherwise, the loader will be init'd when reloadFromArguments
            // is called.
            getLoaderManager().initLoader(SamlibSearchLoader.SEARCH_TOKEN, null, RemoteAuthorsFragment.this);
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        adapter.setCallbacks(sDummyCallbacks);
    }

    private void hideEmptyView() {
        emptyText.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
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
            list.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
        } else {
            // Showing authors as a result of search. If blank - show no resuls
            emptyText.setText(R.string.empty_search_results);
            emptyText.setVisibility(View.VISIBLE);
            list.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<AsyncTaskResult<List<SearchedAuthor>>> onCreateLoader(int id, Bundle data) {
        LOGD(TAG, "onCreateLoader, id=" + id + ", data=" + data);
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(data);
        Uri sessionsUri = intent.getData();
        Loader<AsyncTaskResult<List<SearchedAuthor>>> loader = null;
        if (id == SamlibSearchLoader.SEARCH_TOKEN) {
            LOGD(TAG, "Creating search loader for " + sessionsUri);
            loader = new SamlibSearchLoader(getActivity(), sessionsUri);
        }
        return loader;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void onLoadFinished(Loader<AsyncTaskResult<List<SearchedAuthor>>> loader, AsyncTaskResult<List<SearchedAuthor>> data) {
        if (getActivity() == null) {
            return;
        }

        int token = loader.getId();
        LOGD(TAG, "Loader finished: search");
        if (token == SamlibSearchLoader.SEARCH_TOKEN) {
            if (data.getError() instanceof SearchException) {
                int errorMsg;
                switch (((SearchException) data.getError()).getError()) {
                    case SAMLIB_BUSY:
                        errorMsg = R.string.cannot_search_busy;
                        break;
                    case NETWORK_ERROR:
                        errorMsg = R.string.cannot_search_network;
                        break;
                    case INTERNAL_ERROR:
                        errorMsg = R.string.cannot_search_internal;
                        break;
                    default:
                        errorMsg = R.string.cannot_search_unknown;
                        break;
                }
                Style.Builder alertStyle = new Style.Builder()
                        .setTextAppearance(android.R.attr.textAppearanceLarge)
                        .setPaddingInPixels(25)
                        .setBackgroundColorValue(Style.holoRedLight);
                Crouton.makeText(getActivity(), getString(errorMsg), alertStyle.build()).show();
            }
            updateCollectionView(data.getResult());
        }
    }

    @Override
    public void onLoaderReset(Loader<AsyncTaskResult<List<SearchedAuthor>>> listLoader) {

    }


}
