package com.andrada.sitracker.fragment;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.SettingsActivity_;
import com.andrada.sitracker.contracts.AuthorUpdateStatusListener;
import com.andrada.sitracker.contracts.PublicationMarkedAsReadListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.fragment.adapters.AuthorsAdapter;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog;
import com.andrada.sitracker.tasks.AddAuthorTask;
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

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@EFragment(R.layout.fragmet_authors)
@OptionsMenu(R.menu.authors_menu)
public class AuthorsFragment extends SherlockFragment implements AddAuthorTask.IAuthorTaskCallback,
        AuthorUpdateStatusListener, AddAuthorDialog.OnAuthorLinkSuppliedListener,
        PublicationMarkedAsReadListener, MultiChoiceModeListener, View.OnClickListener {

    public interface OnAuthorSelectedListener {
        public void onAuthorSelected(long id);
    }

    OnAuthorSelectedListener mCallback;

    @ViewById
    ListView list;

    @Bean
    AuthorsAdapter adapter;

    @SystemService
    ConnectivityManager connectivityManager;

    @InstanceState
    int currentAuthorIndex = 0;

    private Crouton mNoNetworkCrouton;

    private boolean mIsUpdating = false;

    private List<Author> mSelectedAuthors = new ArrayList<Author>();

    //region Fragment lifecycle overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        list.setBackgroundResource(R.drawable.authors_list_background);
        getSherlockActivity().invalidateOptionsMenu();
        if (adapter.getCount() > 0 && currentAuthorIndex < adapter.getCount()) {
            listItemClicked(currentAuthorIndex);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            // This makes sure that the container activity has implemented
            // the callback interface. If not, it throws an exception.
            mCallback = (OnAuthorSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnAuthorSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
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
        authorDialog.setOnAuthorLinkSuppliedListener(this);
        authorDialog.show(getActivity().getSupportFragmentManager(),
                Constants.DIALOG_ADD_AUTHOR);
        EasyTracker.getTracker().sendView(Constants.GA_SCREEN_ADD_DIALOG);
    }

    @OptionsItem(R.id.action_refresh)
    void menuRefreshSelected() {
        if (!mIsUpdating) {
            final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                UpdateAuthorsTask_.intent(getActivity()).start();
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

    @OptionsItem(R.id.action_settings)
    void menuSettingsSelected() {
        getSherlockActivity().startActivity(SettingsActivity_.intent(getSherlockActivity()).get());
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
    }

    protected void updateAuthors() {
        int tempPosition = list.getCheckedItemPosition();
        adapter.setSelectedItem(tempPosition);
        adapter.reloadAuthors();
    }

    protected void tryAddAuthor(String url) {
        new AddAuthorTask((Context) mCallback, this).execute(url);
    }

    @ItemClick
    public void listItemClicked(int position) {
        // Notify the parent activity of selected item
        long id = list.getItemIdAtPosition(position);
        currentAuthorIndex = position;
        mCallback.onAuthorSelected(id);

        // Set the item as checked to be highlighted
        adapter.setSelectedItem(position);
        adapter.notifyDataSetChanged();

    }

    private void toggleUpdatingState() {
        mIsUpdating = !mIsUpdating;
        ActionBar bar = ((SherlockFragmentActivity) getActivity()).getSupportActionBar();
        bar.setDisplayShowHomeEnabled(!mIsUpdating);
        bar.setDisplayShowTitleEnabled(!mIsUpdating);
        bar.setDisplayShowCustomEnabled(mIsUpdating);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(mIsUpdating);
        if (mIsUpdating) {
            View mLogoView = LayoutInflater.from(getActivity()).inflate(R.layout.updating_actionbar_layout, null);

            bar.setCustomView(mLogoView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        getSherlockActivity().invalidateOptionsMenu();
    }


    //region Public methods
    public boolean isUpdating() {
        return mIsUpdating;
    }

    //endregion


    //region AddAuthorTask.IAuthorTaskCallback callbacks

    @Override
    public void onAuthorAddCompleted(String message) {
        EasyTracker.getTracker().sendEvent(
                Constants.GA_UI_CATEGORY,
                Constants.GA_EVENT_AUTHOR_ADDED,
                Constants.GA_EVENT_AUTHOR_ADDED, null);
        EasyTracker.getInstance().dispatch();
        //Stop progress bar

        Style.Builder alertStyle = new Style.Builder()
                .setTextAppearance(android.R.attr.textAppearanceLarge)
                .setPaddingInPixels(25);

        if (message.length() == 0) {
            //This is success
            updateAuthors();
            alertStyle.setBackgroundColorValue(Style.holoGreenLight);
            message = getResources().getString(R.string.author_add_success_crouton_message);
        } else {
            alertStyle.setBackgroundColorValue(Style.holoRedLight);
        }
        Crouton.makeText(getSherlockActivity(), message, alertStyle.build()).show();
    }

    //endregion


    //region AuthorUpdateStatusListener callbacks
    @Override
    public void onAuthorsUpdated() {
        if (isUpdating()) {
            toggleUpdatingState();
        }
        this.updateAuthors();
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
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(com.andrada.sitracker.util.actionmodecompat.ActionMode mode) {
    }

    //endregion


    //region AddAuthorDialog.OnAuthorLinkSuppliedListener callbacks
    @Override
    public void onLinkSupplied(String url) {
        tryAddAuthor(url);
    }
    //endregion


    @Override
    public void onPublicationMarkedAsRead(long publicationId) {
        //ensure we update the new status of the author if he has no new publications
        EasyTracker.getTracker().sendEvent(
                Constants.GA_UI_CATEGORY,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ, null);
        EasyTracker.getInstance().dispatch();
        adapter.notifyDataSetChanged();

    }

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
