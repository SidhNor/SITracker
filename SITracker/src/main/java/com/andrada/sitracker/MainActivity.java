package com.andrada.sitracker;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.AuthorsFragment.OnAuthorSelectedListener;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog;
import com.andrada.sitracker.phoneactivities.PublicationsActivity_;
import com.andrada.sitracker.task.UpdateAuthorsIntentService_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main)
public class MainActivity extends SherlockFragmentActivity implements
        OnAuthorSelectedListener, AddAuthorDialog.OnAuthorLinkSuppliedListener {

    @FragmentById(R.id.fragment_publications)
    PublicationsFragment mPubFragment;

    @FragmentById(R.id.fragment_authors)
    AuthorsFragment mAuthorsFragment;

    private boolean mDualFragments = false;

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
    }
    //endregion

}
