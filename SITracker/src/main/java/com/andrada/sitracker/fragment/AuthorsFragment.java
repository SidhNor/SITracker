package com.andrada.sitracker.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

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

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

@EFragment(R.layout.fragmet_authors)
@OptionsMenu(R.menu.authors_menu)
public class AuthorsFragment extends SherlockFragment implements AddAuthorTask.IAuthorTaskCallback,
        AuthorUpdateStatusListener, AddAuthorDialog.OnAuthorLinkSuppliedListener,
        PublicationMarkedAsReadListener, MultiChoiceModeListener {

    public interface OnAuthorSelectedListener {
        public void onAuthorSelected(long id);
    }

    OnAuthorSelectedListener mCallback;

    @ViewById
    ListView list;

    @ViewById
    View refreshProgressBar;

    @Bean
    AuthorsAdapter adapter;

    @InstanceState
    int currentAuthorIndex = 0;

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

    //region Menu item tap handlers
    @OptionsItem(R.id.action_add)
    void menuAddSelected() {
        AddAuthorDialog authorDialog = new AddAuthorDialog();
        authorDialog.setOnAuthorLinkSuppliedListener(this);
        authorDialog.show(getActivity().getSupportFragmentManager(),
                Constants.DIALOG_ADD_AUTHOR);
    }

    @OptionsItem(R.id.action_refresh)
    void menuRefreshSelected() {
        UpdateAuthorsTask_.intent(getActivity()).start();
        toggleUpdatingState();
    }

    @OptionsItem(R.id.action_settings)
    void menuSettingsSelected() {
        getSherlockActivity().startActivity(SettingsActivity_.intent(getSherlockActivity()).get());
    }
    //endregion

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
        ActionBar bar = ((SherlockFragmentActivity) getActivity()).getSupportActionBar();
        bar.setDisplayShowHomeEnabled(mIsUpdating);
        bar.setDisplayShowTitleEnabled(mIsUpdating);
        bar.setDisplayShowCustomEnabled(!mIsUpdating);

        if (mIsUpdating) {
            refreshProgressBar.setVisibility(View.GONE);
        } else {
            View mLogoView = LayoutInflater.from(getActivity()).inflate(R.layout.updating_actionbar_layout, null);

            bar.setCustomView(mLogoView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            refreshProgressBar.setVisibility(View.VISIBLE);
        }
        mIsUpdating = !mIsUpdating;
        getSherlockActivity().invalidateOptionsMenu();
    }


    //region Public methods
    public boolean isUpdating() {
        return mIsUpdating;
    }

    //endregion


    //region AddAuthorTask.IAuthorTaskCallback callbacks
    @Override
    public void onAuthorAddStarted() {
        //Add a temporary item to authors
        //Start progress bar
    }

    @Override
    public void onAuthorAddCompleted(String message) {
        //Stop progress bar
        if (message.length() == 0) {
            //This is success
            updateAuthors();
        } else {
            Toast.makeText((Context) mCallback, message, Toast.LENGTH_SHORT).show();
        }
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
        //TODO Show failed notification/toast
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
        adapter.notifyDataSetChanged();
    }

}
