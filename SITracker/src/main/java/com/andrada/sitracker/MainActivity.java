package com.andrada.sitracker;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.AuthorsFragment.OnAuthorSelectedListener;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.phoneactivities.PublicationsActivity_;
import com.andrada.sitracker.task.receivers.UpdateBroadcastReceiver;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsMenu;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main_menu)
public class MainActivity extends SherlockFragmentActivity implements
        OnAuthorSelectedListener {

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

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        //Do not show menu in actionbar if authors are updating
        if (mAuthorsFragment != null) {
            return !mAuthorsFragment.isUpdating();
        }
        return true;
    }

    private BroadcastReceiver receiver;

    @Override
    protected void onResume() {
        if (receiver == null) {
            //AuthorsFragment is the callback here
            receiver = new UpdateBroadcastReceiver(mAuthorsFragment);
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

}
