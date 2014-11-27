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
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.ListView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AuthorUpdateStatusListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.events.AuthorCheckedEvent;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.events.AuthorSortMethodChanged;
import com.andrada.sitracker.events.BackUpRestoredEvent;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.tasks.filters.UpdateStatusMessageFilter;
import com.andrada.sitracker.tasks.receivers.UpdateStatusReceiver;
import com.andrada.sitracker.ui.MultiSelectionUtil;
import com.andrada.sitracker.ui.SearchActivity_;
import com.andrada.sitracker.ui.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.UpdateServiceHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.andrada.sitracker.util.LogUtils.LOGI;

@EFragment(R.layout.fragment_myauthors)
@OptionsMenu(R.menu.authors_menu)
public class AuthorsFragment extends BaseListFragment implements
        AuthorUpdateStatusListener, MultiSelectionUtil.MultiChoiceModeListener,
        NavDrawerManager.NavDrawerItemAware {

    private final ArrayList<Long> mSelectedAuthors = new ArrayList<Long>();
    @ViewById(R.id.authors_list)
    ListView list;
    @ViewById
    ViewStub empty;
    @Bean
    AuthorsAdapter adapter;
    @SystemService
    ConnectivityManager connectivityManager;
    @InstanceState
    long currentAuthorIndex = -1;
    boolean mIsUpdating = false;

    @Nullable
    @InstanceState
    long[] checkedItems;
    @Nullable
    private Crouton mNoNetworkCrouton;
    @Nullable
    private MultiSelectionUtil.Controller mMultiSelectionController;

    private BroadcastReceiver updateStatusReceiver;

    //region Fragment lifecycle overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGI("SITracker", "AuthorsFragment - OnCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        getBaseActivity().getActionBarUtil().enableActionBarAutoHide(getScrollingView());
        getBaseActivity().getActionBarUtil().registerHideableHeaderView(getActivity().findViewById(R.id.headerbar));

        //Set title
        getBaseActivity().getDrawerManager().pushNavigationalState(getString(R.string.navdrawer_item_my_authors), true);

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
        getBaseActivity().getActionBarUtil().disableActionBarAutoHide();
        getBaseActivity().getActionBarUtil().autoShowOrHideActionBar(true);
        getBaseActivity().getActionBarUtil().deregisterHideableHeaderView(getActivity().findViewById(R.id.headerbar));
        EventBus.getDefault().unregister(this);
        getActivity().unregisterReceiver(updateStatusReceiver);
    }

    @Override
    public void onDestroy() {
        if (mMultiSelectionController != null) {
            mMultiSelectionController.finish();
        }
        mMultiSelectionController = null;
        super.onDestroy();
        LOGI("SITracker", "AuthorsFragment - OnDestroy");
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
        if (!mIsUpdating && adapter.getCount() > 0) {
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
                showNoNetworkCroutonMessage();
            }
        }
    }

    //endregion

    /**
     * Crouton click handler
     *
     * @param view being clicked
     */
    public void onCroutonClick(@NotNull View view) {
        if (view.getId() == R.id.retryUpdateButton) {
            if (this.mNoNetworkCrouton != null) {
                Crouton.hide(this.mNoNetworkCrouton);
                this.mNoNetworkCrouton = null;
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
    }

    @AfterViews
    void bindAdapter() {
        list.setAdapter(adapter);
        mMultiSelectionController = MultiSelectionUtil.attachMultiSelectionController(
                list,
                (ActionBarActivity) getActivity(),
                this);
        //ActionMode.setMultiChoiceMode(list, getActivity(), this);
        list.setBackgroundResource(R.drawable.authors_list_background);
        empty.setLayoutResource(R.layout.empty_authors);
        list.setEmptyView(empty);
        mMultiSelectionController.tryRestoreInstanceState(checkedItems);
    }

    @ItemClick(R.id.authors_list)
    public void listItemClicked(int position) {
        // Notify the parent activity of selected item
        currentAuthorIndex = list.getItemIdAtPosition(position);
        // Set the item as checked to be highlighted
        adapter.setSelectedItem(currentAuthorIndex);
        String name = ((Author) adapter.getItem(position)).getName();
        EventBus.getDefault().post(new AuthorSelectedEvent(adapter.getSelectedAuthorId(), name));
        adapter.notifyDataSetChanged();
    }

    private void toggleUpdatingState() {
        mIsUpdating = !mIsUpdating;
        onRefreshingStateChanged(mIsUpdating);
    }


    //region Public methods
    public boolean isUpdating() {
        return UpdateServiceHelper.isServiceCurrentlyRunning(getActivity().getApplicationContext());
    }

    public String getCurrentSelectedAuthorName() {
        String name = "";
        if (adapter.getCurrentlySelectedAuthor() != null) {
            name = adapter.getCurrentlySelectedAuthor().getName();
        }
        return name;
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
        Crouton.makeText(getActivity(),
                getResources().getText(R.string.update_failed_crouton_message),
                Style.ALERT).show();
    }
    //endregion

    //region CABListener

    @Override
    public void onItemCheckedStateChanged(@NotNull ActionMode mode,
                                          int position, long id, boolean checked) {
        if (checked) {
            mSelectedAuthors.add(((Author) adapter.getItem(position)).getId());
        } else {
            mSelectedAuthors.remove(((Author) adapter.getItem(position)).getId());
        }
        int numSelectedAuthors = mSelectedAuthors.size();
        mode.setTitle(getResources().getQuantityString(
                R.plurals.authors_selected,
                numSelectedAuthors, numSelectedAuthors));
        checkedItems = ArrayUtils.toPrimitive(mSelectedAuthors.toArray(new Long[mSelectedAuthors.size()]));
    }

    @Override
    public boolean onCreateActionMode(@NotNull ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.context_authors, menu);
        mSelectedAuthors.clear();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(@NotNull ActionMode mode, @NotNull MenuItem item) {
        mode.finish();
        if (item.getItemId() == R.id.action_remove) {
            AnalyticsHelper.getInstance().sendEvent(
                    Constants.GA_ADMIN_CATEGORY,
                    Constants.GA_EVENT_AUTHOR_REMOVED,
                    Constants.GA_EVENT_AUTHOR_REMOVED, (long) mSelectedAuthors.size());

            //This stuff is on background thread
            adapter.removeAuthors(mSelectedAuthors);
            return true;
        } else if (item.getItemId() == R.id.action_mark_read) {
            adapter.markAuthorsRead(mSelectedAuthors);
            AnalyticsHelper.getInstance().sendEvent(
                    Constants.GA_ADMIN_CATEGORY,
                    Constants.GA_EVENT_AUTHOR_MANUAL_READ,
                    Constants.GA_EVENT_AUTHOR_MANUAL_READ, (long) mSelectedAuthors.size());

            BackupManager bm = new BackupManager(getActivity());
            bm.dataChanged();
            return true;
        } else if (item.getItemId() == R.id.action_open_authors_browser) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (mSelectedAuthors.contains(adapter.getItemId(i))) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(((Author) adapter.getItem(i)).getUrl()));
                    getActivity().startActivity(intent);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedItems = null;
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

    public void onEvent(AuthorSelectedEvent event) {
        //TODO why is this even here?
        /*if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {
            mRecyclerView.scrollToPosition(0);
        }*/
    }

    public void onEvent(BackUpRestoredEvent event) {
        if (adapter != null) {
            adapter.reloadAuthors();
            this.showSuccessfulRestore();
        }
    }

    public void onEvent(AuthorCheckedEvent event) {
        if (mMultiSelectionController != null) {
            mMultiSelectionController.startActionModeOrSelectId(event.authorId, event.view);
        }
    }

    @UiThread
    protected void showSuccessfulRestore() {
        String message = getResources().getString(R.string.backup_restored_crouton_message);
        Style.Builder alertStyle = new Style.Builder()
                .setTextAppearance(android.R.attr.textAppearanceLarge)
                .setPaddingInPixels(25);
        alertStyle.setBackgroundColorValue(Style.holoGreenLight);
        Crouton.makeText(getActivity(), message, alertStyle.build()).show();
    }


    //region AuthorAddedEvent handler

    @SuppressWarnings("UnusedParameters")
    public void onEvent(AuthorSortMethodChanged event) {
        adapter.reloadAuthors();
    }
    //endregion


    private void showNoNetworkCroutonMessage() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.crouton_no_network, null);
        view.findViewById(R.id.retryUpdateButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCroutonClick(v);
            }
        });
        Configuration croutonConfiguration = new Configuration.Builder()
                .setDuration(Configuration.DURATION_LONG).build();
        this.mNoNetworkCrouton = Crouton.make(getActivity(), view);
        this.mNoNetworkCrouton.setConfiguration(croutonConfiguration);
        this.mNoNetworkCrouton.show();
    }

    public AuthorsAdapter getAdapter() {
        return adapter;
    }

    @Override
    public ListView getScrollingView() {
        return list;
    }

    @Override
    public void setContentTopClearance(int clearance) {
        super.setContentTopClearance(clearance);
        if (list != null) {
            list.setPadding(list.getPaddingLeft(), clearance,
                    list.getPaddingRight(), list.getPaddingBottom());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return ViewCompat.canScrollVertically(list, -1);
    }

    @Override
    public int getSelfNavDrawerItem() {
        return NavDrawerManager.NAVDRAWER_ITEM_MY_AUTHORS;
    }
}
