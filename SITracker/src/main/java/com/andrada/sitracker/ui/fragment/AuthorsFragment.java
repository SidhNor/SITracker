/*
 *
 * Copyright 2016 Gleb Godonoga.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.andrada.sitracker.ui.fragment;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AuthorItemListener;
import com.andrada.sitracker.contracts.AuthorUpdateStatusListener;
import com.andrada.sitracker.events.AuthorCheckedEvent;
import com.andrada.sitracker.events.AuthorSortMethodChanged;
import com.andrada.sitracker.events.BackUpRestoredEvent;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.tasks.filters.UpdateStatusMessageFilter;
import com.andrada.sitracker.tasks.receivers.UpdateStatusReceiver;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.SearchActivity_;
import com.andrada.sitracker.ui.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.ui.widget.AuthorMultiSelector;
import com.andrada.sitracker.ui.widget.DividerItemDecoration;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.UpdateServiceHelper;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

import static com.andrada.sitracker.util.LogUtils.LOGI;

@EFragment(R.layout.fragment_myauthors)
@OptionsMenu(R.menu.authors_menu)
public class AuthorsFragment extends BaseListFragment implements
        AuthorUpdateStatusListener, AuthorItemListener {

    private static final String TAG = "authorListFragment";

    @ViewById(R.id.authors_list)
    RecyclerView list;

    @ViewById
    ViewStub empty;

    @Bean
    AuthorsAdapter adapter;

    @SystemService
    ConnectivityManager connectivityManager;

    boolean mIsUpdating = false;

    @Nullable
    private Snackbar mNoNetworkSnack;

    /**
     * This is a singletone bean
     */
    @Bean
    AuthorMultiSelector mMultiSelector;

    private ModalMultiSelectorCallback mDeleteMode;
    private ActionMode currentActionMode;


    private BroadcastReceiver updateStatusReceiver;

    //region Fragment lifecycle overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        LOGI("SITracker", "AuthorsFragment - OnCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        ((BaseActivity) getActivity()).getActionBarToolbar().setTitle(getString(R.string.navdrawer_item_my_authors));

        //Receiver registration
        mIsUpdating = UpdateServiceHelper.isServiceCurrentlyRunning(getActivity().getApplicationContext());

        if (updateStatusReceiver == null) {
            updateStatusReceiver = new UpdateStatusReceiver(this);
            updateStatusReceiver.setOrderedHint(true);
        }
        UpdateStatusMessageFilter filter = new UpdateStatusMessageFilter();
        filter.setPriority(1);
        getActivity().registerReceiver(updateStatusReceiver, filter);

        //Reload authors
        adapter.reloadAuthors();

        onRefreshingStateChanged(mIsUpdating);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        getActivity().unregisterReceiver(updateStatusReceiver);
        if (currentActionMode != null) {
            currentActionMode.finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBundle(TAG, mMultiSelector.saveSelectionStates());
        super.onSaveInstanceState(outState);
    }

    @Override
    @UiThread(delay = 100)
    protected void onRefreshingStateChanged(boolean refreshing) {
        super.onRefreshingStateChanged(refreshing);
    }


    //endregion
    //region Menu item tap handlers
    @OptionsItem(R.id.action_search)
    void menuSearchSelected() {
        AnalyticsHelper.getInstance().sendEvent(Constants.GA_EXPLORE_CATEGORY, "launchsearch", "");
        SearchActivity_.intent(this.getActivity()).start();
    }

    protected void requestDataRefresh() {
        if (mIsUpdating) {
            return;
        }
        performManualRefresh();
    }

    void performManualRefresh() {
        if (!mIsUpdating && adapter.getItemCount() > 0) {
            final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                Intent updateIntent = new Intent(getActivity(), UpdateAuthorsTask_.class);
                updateIntent.putExtra(Constants.UPDATE_IGNORES_NETWORK, true);
                getActivity().startService(updateIntent);
                AnalyticsHelper.getInstance().sendEvent(
                        Constants.GA_READ_CATEGORY,
                        Constants.GA_EVENT_AUTHORS_MANUAL_REFRESH,
                        Constants.GA_EVENT_AUTHORS_MANUAL_REFRESH);

                //Start refreshing
                toggleUpdatingState();
            } else {
                //Surface crouton that network is unavailable
                showNoNetworkSnackbar();
            }
        }
    }

    //endregion

    /**
     * Snackbar click handler
     */
    private View.OnClickListener snackBarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mNoNetworkSnack != null) {
                mNoNetworkSnack.dismiss();
                mNoNetworkSnack = null;
            }
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                performManualRefresh();
                            }
                        });
                    }
                }
            }, 1500);
        }
    };

    @AfterInject
    void bindMultiSelect() {
        mDeleteMode = new ModalMultiSelectorCallback(mMultiSelector) {

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                super.onCreateActionMode(actionMode, menu);
                getActivity().getMenuInflater().inflate(R.menu.context_authors, menu);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                mode.finish();
                currentActionMode = null;
                List<Long> selectedAuthors = new LinkedList<>();
                for (Integer position : mMultiSelector.getSelectedPositions()) {
                    selectedAuthors.add(adapter.getItemId(position));
                }

                if (item.getItemId() == R.id.action_remove) {
                    AnalyticsHelper.getInstance().sendEvent(
                            Constants.GA_ADMIN_CATEGORY,
                            Constants.GA_EVENT_AUTHOR_REMOVED,
                            Constants.GA_EVENT_AUTHOR_REMOVED, (long) selectedAuthors.size());

                    //This stuff is on background thread
                    adapter.removeAuthors(selectedAuthors);
                    mMultiSelector.clearSelections();
                    return true;
                } else if (item.getItemId() == R.id.action_mark_read) {
                    adapter.markAuthorsRead(selectedAuthors);
                    AnalyticsHelper.getInstance().sendEvent(
                            Constants.GA_ADMIN_CATEGORY,
                            Constants.GA_EVENT_AUTHOR_MANUAL_READ,
                            Constants.GA_EVENT_AUTHOR_MANUAL_READ, (long) selectedAuthors.size());

                    BackupManager bm = new BackupManager(getActivity());
                    bm.dataChanged();
                    mMultiSelector.clearSelections();
                    return true;
                } else if (item.getItemId() == R.id.action_open_authors_browser) {
                    for (int i = 0; i < adapter.getItemCount(); i++) {
                        if (selectedAuthors.contains(adapter.getItemId(i))) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(adapter.getItem(i).getUrl()));
                            getActivity().startActivity(intent);
                        }
                    }
                    mMultiSelector.clearSelections();
                    return true;
                }
                return false;
            }
        };
    }


    @AfterViews
    void bindAdapter() {
        adapter.updateContext(getBaseActivity());
        adapter.setAuthorItemListener(this);
        list.setLayoutManager(new LinearLayoutManager(list.getContext()));
        list.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        list.setAdapter(adapter);

        list.setBackgroundResource(R.drawable.authors_list_background);

        //TODO handle empty
        //empty.setLayoutResource(R.layout.empty_authors);
        //list.setEmptyView(empty);

        if (mMultiSelector.isSelectable()) {
            if (mDeleteMode != null) {
                mDeleteMode.setClearOnPrepare(false);
                currentActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mDeleteMode);
            }
        }
    }

    private void toggleUpdatingState() {
        mIsUpdating = !mIsUpdating;
        onRefreshingStateChanged(mIsUpdating);
    }


    //region Public methods
    public boolean isUpdating() {
        return UpdateServiceHelper.isServiceCurrentlyRunning(getActivity().getApplicationContext());
    }

    //endregion

    //region AuthorUpdateStatusListener callbacks
    @Override
    public void onAuthorsUpdated() {
        toggleUpdatingState();
        adapter.reloadAuthors();
    }

    @Override
    public void onAuthorsUpdateFailed() {
        toggleUpdatingState();
        //surface crouton that update failed
        SpannableStringBuilder snackbarText = new SpannableStringBuilder();
        snackbarText.append(getResources().getText(R.string.update_failed_crouton_message));
        snackbarText.setSpan(new ForegroundColorSpan(0xFFFF0000), 0, snackbarText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Snackbar.make(getActivity().findViewById(R.id.drawer_layout), snackbarText, Snackbar.LENGTH_SHORT).show();

    }

    @Override
    public void onAuthorsUpToDate() {
        toggleUpdatingState();
        Snackbar.make(getActivity().findViewById(R.id.drawer_layout), R.string.authors_up_to_date, Snackbar.LENGTH_SHORT).show();
    }
    //endregion


    public void onEvent(@NotNull PublicationMarkedAsReadEvent event) {
        //ensure we update the new status of the author if he has no new publications
        AnalyticsHelper.getInstance().sendEvent(
                Constants.GA_READ_CATEGORY,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ);
        if (event.refreshAuthor) {
            adapter.reloadAuthors();
        }
    }

    public void onEvent(BackUpRestoredEvent event) {
        if (adapter != null) {
            adapter.reloadAuthors();
            this.showSuccessfulRestore();
        }
    }

    public void onEvent(AuthorCheckedEvent event) {
        if (!mMultiSelector.isSelectable()) {
            currentActionMode = getBaseActivity().startSupportActionMode(mDeleteMode);
        }
        final int currentPosition = adapter.getItemPositionByAuthorId(event.authorId);
        final boolean currentlyState = mMultiSelector.isSelected(currentPosition, event.authorId);
        mMultiSelector.setSelected(currentPosition, event.authorId, !currentlyState);
        updateActionModeTitle();
    }

    @UiThread
    protected void showSuccessfulRestore() {
        String message = getResources().getString(R.string.backup_restored_crouton_message);
        Snackbar.make(getActivity().findViewById(R.id.drawer_layout), message, Snackbar.LENGTH_SHORT).show();
    }


    //region AuthorAddedEvent handler

    @SuppressWarnings("UnusedParameters")
    public void onEvent(AuthorSortMethodChanged event) {
        adapter.reloadAuthors();
    }
    //endregion


    private void showNoNetworkSnackbar() {
        String msg = getResources().getString(R.string.no_network_error);
        Snackbar.make(getActivity().findViewById(R.id.drawer_layout), msg, Snackbar.LENGTH_LONG)
                .setAction(getResources().getString(R.string.no_network_retry), this.snackBarClickListener)
                .show();
    }

    public AuthorsAdapter getAdapter() {
        return adapter;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return ViewCompat.canScrollVertically(list, -1);
    }

    @Override
    public void onAuthorItemClick() {
        updateActionModeTitle();
    }

    @Override
    public void onAuthorItemLongClick(AuthorsAdapter.ViewHolder viewHolder) {
        if (!mMultiSelector.isSelectable()) {
            currentActionMode = getBaseActivity().startSupportActionMode(mDeleteMode);
        }
        final boolean currentState = mMultiSelector.isSelected(viewHolder.getAdapterPosition(), viewHolder.getItemId());
        mMultiSelector.setSelected(viewHolder, !currentState);
        updateActionModeTitle();
    }

    private void updateActionModeTitle() {
        int numSelectedAuthors = mMultiSelector.getSelectedPositions().size();
        if (currentActionMode != null) {
            if (numSelectedAuthors == 0) {
                currentActionMode.finish();
            } else {
                currentActionMode.setTitle(getResources().getQuantityString(
                        R.plurals.authors_selected,
                        numSelectedAuthors, numSelectedAuthors));
            }
        }
    }
}
