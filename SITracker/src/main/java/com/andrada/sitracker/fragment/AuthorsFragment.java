package com.andrada.sitracker.fragment;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AuthorUpdateStatusListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.events.AuthorAddedEvent;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.events.ProgressBarToggleEvent;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.util.actionmodecompat.ActionMode;
import com.andrada.sitracker.util.actionmodecompat.MultiChoiceModeListener;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@EFragment(R.layout.fragment_authors)
@OptionsMenu(R.menu.authors_menu)
public class AuthorsFragment extends SherlockFragment implements AuthorUpdateStatusListener,
        MultiChoiceModeListener, View.OnClickListener {

    @ViewById
    ListView list;

    @ViewById
    ViewStub empty;

    @Bean
    AuthorsAdapter adapter;

    @SystemService
    ConnectivityManager connectivityManager;

    @InstanceState
    long currentAuthorIndex = -1;

    private Crouton mNoNetworkCrouton;

    private boolean mIsUpdating = false;

    private List<Author> mSelectedAuthors = new ArrayList<Author>();

    //region Fragment lifecycle overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        mIsUpdating = false;
        getSherlockActivity().invalidateOptionsMenu();
        currentAuthorIndex = currentAuthorIndex == -1 ? adapter.getFirstAuthorId() : currentAuthorIndex;
        EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
        // Set the item as checked to be highlighted
        adapter.setSelectedItem(currentAuthorIndex);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    //endregion

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        if (mIsUpdating) {
            menu.removeItem(R.id.action_refresh);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    //region Menu item tap handlers
    @OptionsItem(R.id.action_add)
    void menuAddSelected() {
        AddAuthorDialog authorDialog = new AddAuthorDialog();
        authorDialog.show(getActivity().getSupportFragmentManager(),
                Constants.DIALOG_ADD_AUTHOR);
        EasyTracker.getTracker().sendView(Constants.GA_SCREEN_ADD_DIALOG);
    }

    @OptionsItem(R.id.action_refresh)
    void menuRefreshSelected() {
        if (!mIsUpdating && adapter.getCount() > 0) {
            final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                Intent updateIntent = new Intent(getSherlockActivity(), UpdateAuthorsTask_.class);
                updateIntent.putExtra(Constants.UPDATE_IGNORES_NETWORK, true);
                getSherlockActivity().startService(updateIntent);
                EasyTracker.getTracker().sendEvent(
                        Constants.GA_UI_CATEGORY,
                        Constants.GA_EVENT_AUTHORS_MANUAL_REFRESH,
                        Constants.GA_EVENT_AUTHORS_MANUAL_REFRESH, null);
                EasyTracker.getInstance().dispatch();
                toggleUpdatingState();
            } else {
                //Surface crouton that network is unavailable
                showNoNetworkCroutonMessage();
            }
        }
    }

    //endregion

    @Override
    /**
     * Crouton click handler
     */
    public void onClick(View view) {
        if (view.getId() == R.id.retryUpdateButton) {
            if (this.mNoNetworkCrouton != null) {
                Crouton.hide(this.mNoNetworkCrouton);
                this.mNoNetworkCrouton = null;
            }
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    menuRefreshSelected();
                }
            }, 1500);
        }

    }

    @AfterViews
    void bindAdapter() {
        list.setAdapter(adapter);
        ActionMode.setMultiChoiceMode(list, getSherlockActivity(), this);
        list.setBackgroundResource(R.drawable.authors_list_background);
        list.setEmptyView(empty);
    }

    @ItemClick
    public void listItemClicked(int position) {
        // Notify the parent activity of selected item
        currentAuthorIndex = list.getItemIdAtPosition(position);
        EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
        // Set the item as checked to be highlighted
        adapter.setSelectedItem(currentAuthorIndex);
        adapter.notifyDataSetChanged();

    }

    private void toggleUpdatingState() {
        mIsUpdating = !mIsUpdating;
        ActionBar bar = ((SherlockFragmentActivity) getActivity()).getSupportActionBar();
        bar.setDisplayShowHomeEnabled(!mIsUpdating);
        bar.setDisplayShowTitleEnabled(!mIsUpdating);
        bar.setDisplayShowCustomEnabled(mIsUpdating);

        EventBus.getDefault().post(new ProgressBarToggleEvent(mIsUpdating));
        if (mIsUpdating) {
            View mLogoView = LayoutInflater.from(getActivity()).inflate(R.layout.updating_actionbar_layout, null);

            bar.setCustomView(mLogoView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mLogoView.clearAnimation();
            mLogoView.startAnimation(AnimationUtils.loadAnimation(this.getSherlockActivity(), R.anim.ab_custom_view_anim));
        }
        getSherlockActivity().invalidateOptionsMenu();
    }


    //region Public methods
    public boolean isUpdating() {
        return mIsUpdating;
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
        if (isUpdating()) {
            toggleUpdatingState();
        }
        adapter.reloadAuthors();
    }

    @Override
    public void onAuthorsUpdateFailed() {
        toggleUpdatingState();
        //surface crouton that update failed
        Crouton.makeText(getSherlockActivity(),
                getResources().getText(R.string.update_failed_crouton_message),
                Style.ALERT).show();
    }
    //endregion

    //region CABListener

    @Override
    public void onItemCheckedStateChanged(com.andrada.sitracker.util.actionmodecompat.ActionMode mode,
                                          int position, long id, boolean checked) {
        if (checked) {
            mSelectedAuthors.add((Author) adapter.getItem(position));
        } else {
            mSelectedAuthors.remove(adapter.getItem(position));
        }
        int numSelectedAuthors = mSelectedAuthors.size();
        mode.setTitle(getResources().getQuantityString(
                R.plurals.authors_selected,
                numSelectedAuthors, numSelectedAuthors));

    }

    @Override
    public boolean onCreateActionMode(com.andrada.sitracker.util.actionmodecompat.ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.context, menu);
        mSelectedAuthors.clear();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(com.andrada.sitracker.util.actionmodecompat.ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(com.andrada.sitracker.util.actionmodecompat.ActionMode mode, MenuItem item) {
        mode.finish();
        if (item.getItemId() == R.id.action_remove) {
            EasyTracker.getTracker().sendEvent(
                    Constants.GA_UI_CATEGORY,
                    Constants.GA_EVENT_AUTHOR_REMOVED,
                    Constants.GA_EVENT_AUTHOR_REMOVED, (long) mSelectedAuthors.size());
            EasyTracker.getInstance().dispatch();
            adapter.removeAuthors(mSelectedAuthors);
            currentAuthorIndex = adapter.getSelectedAuthorId();
            EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(com.andrada.sitracker.util.actionmodecompat.ActionMode mode) {
    }

    //endregion

    public void onEvent(PublicationMarkedAsReadEvent publicationId) {
        //ensure we update the new status of the author if he has no new publications
        EasyTracker.getTracker().sendEvent(
                Constants.GA_UI_CATEGORY,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ, null);
        EasyTracker.getInstance().dispatch();
        adapter.notifyDataSetChanged();
    }


    //region AuthorAddedEvent handler

    public void onEvent(AuthorAddedEvent event) {
        EasyTracker.getTracker().sendEvent(
                Constants.GA_UI_CATEGORY,
                Constants.GA_EVENT_AUTHOR_ADDED,
                Constants.GA_EVENT_AUTHOR_ADDED, null);
        EasyTracker.getInstance().dispatch();

        EventBus.getDefault().post(new ProgressBarToggleEvent(false));
        String message = event.message;

        //Stop progress bar

        Style.Builder alertStyle = new Style.Builder()
                .setTextAppearance(android.R.attr.textAppearanceLarge)
                .setPaddingInPixels(25);

        if (message.length() == 0) {
            //This is success
            adapter.reloadAuthors();
            alertStyle.setBackgroundColorValue(Style.holoGreenLight);
            message = getResources().getString(R.string.author_add_success_crouton_message);
        } else {
            alertStyle.setBackgroundColorValue(Style.holoRedLight);
        }
        Crouton.makeText(getSherlockActivity(), message, alertStyle.build()).show();

        if (currentAuthorIndex == -1) {
            currentAuthorIndex = adapter.getFirstAuthorId();
            EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
            // Set the item as checked to be highlighted
            adapter.setSelectedItem(currentAuthorIndex);
            adapter.notifyDataSetChanged();
        }
    }

    //endregion


    private void showNoNetworkCroutonMessage() {
        View view = getLayoutInflater(null).inflate(R.layout.crouton_no_network, null);
        view.findViewById(R.id.retryUpdateButton).setOnClickListener(this);
        Configuration croutonConfiguration = new Configuration.Builder()
                .setDuration(Configuration.DURATION_LONG).build();
        this.mNoNetworkCrouton = Crouton.make(getActivity(), view);
        this.mNoNetworkCrouton.setConfiguration(croutonConfiguration);
        this.mNoNetworkCrouton.show();
    }

}
