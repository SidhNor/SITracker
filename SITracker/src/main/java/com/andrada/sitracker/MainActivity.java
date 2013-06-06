package com.andrada.sitracker;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.andrada.sitracker.contracts.AuthorUpdateProgressListener;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.AuthorsFragment.OnAuthorSelectedListener;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog;
import com.andrada.sitracker.phoneactivities.PublicationsActivity_;
import com.andrada.sitracker.task.UpdateAuthorsIntentService_;
import com.andrada.sitracker.task.receivers.UpdateBroadcastReceiver;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main)
public class MainActivity extends SherlockFragmentActivity implements
        OnAuthorSelectedListener, AddAuthorDialog.OnAuthorLinkSuppliedListener,
        AuthorUpdateProgressListener {

    @FragmentById(R.id.fragment_publications)
    PublicationsFragment mPubFragment;

    @FragmentById(R.id.fragment_authors)
    AuthorsFragment mAuthorsFragment;

    @ViewById
    View refreshProgressBar;

    private boolean mDualFragments = false;
    private boolean mIsUpdating = false;

    @AfterViews
    void checkDualFragments() {
        if (mPubFragment != null) {
            mDualFragments = true;
            mAuthorsFragment.setInTwoPane(true);
        }
    }

    @Override
    public void onAuthorSelected(long id) {

        if (!mDualFragments) {
            // If showing only the AuthorsFragment, start the PublicationsActivity and
            // pass it the info about the selected item
            PublicationsActivity_.intent(this).mAuthorId(id).start();
        } else {
            // Capture the publications fragment from the activity layout
            mPubFragment.updatePublicationsView(id);
        }
    }

    protected void updateAuthors() {
        mAuthorsFragment.updateViewAtPosition(mAuthorsFragment.getSelectedItemPosition());
    }

    @Override
    public void onLinkSupplied(String url) {
        mAuthorsFragment.tryAddAuthor(url);
    }

    @Override
    public void onAuthorAdded() {
        updateAuthors();
    }


    //region Menu item tap handlers
    @OptionsItem(R.id.action_add)
    void menuAddSelected() {
        AddAuthorDialog authorDialog = new AddAuthorDialog();
        authorDialog.setOnAuthorLinkSuppliedListener(this);
        authorDialog.show(getSupportFragmentManager(),
                Constants.DIALOG_ADD_AUTHOR);
    }

    @OptionsItem(R.id.action_refresh)
    void menuRefreshSelected() {
        UpdateAuthorsIntentService_.intent(getApplication()).start();
        updateStarted();
    }
    //endregion

    public boolean onPrepareOptionsMenu (Menu menu) {
        //Do not show menu in actionbar if authors are updating
        return !mIsUpdating;
    }

    public void updateStarted() {
        toggleUpdatingState();
    }


    @Override
    public void updateComplete() {
        toggleUpdatingState();
        updateAuthors();
    }

    private BroadcastReceiver receiver;

    @Override
    protected void onResume() {
        if (receiver == null) {
            receiver = new UpdateBroadcastReceiver();
        }
        IntentFilter filter = new IntentFilter(UpdateBroadcastReceiver.UPDATE_RECEIVER_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    private void toggleUpdatingState() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(mIsUpdating);
        bar.setDisplayShowTitleEnabled(mIsUpdating);
        bar.setDisplayShowCustomEnabled(!mIsUpdating);

        if (mIsUpdating) {
            refreshProgressBar.setVisibility(View.GONE);
        } else {
            View mLogoView = LayoutInflater.from(this).inflate(R.layout.updating_actionbar_layout, null);
            bar.setCustomView(mLogoView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            refreshProgressBar.setVisibility(View.VISIBLE);
        }

        mIsUpdating = !mIsUpdating;
        invalidateOptionsMenu();
    }

}
