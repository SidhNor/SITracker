package com.andrada.sitracker;

import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SlidingPaneLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.AuthorsFragment.OnAuthorSelectedListener;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.tasks.filters.MarkAsReadMessageFilter;
import com.andrada.sitracker.tasks.filters.UpdateStatusMessageFilter;
import com.andrada.sitracker.tasks.receivers.MarkedAsReadReceiver;
import com.andrada.sitracker.tasks.receivers.UpdateStatusReceiver;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main_menu)
public class MainActivity extends SherlockFragmentActivity implements
        OnAuthorSelectedListener {

    @FragmentById(R.id.fragment_publications)
    PublicationsFragment mPubFragment;

    @FragmentById(R.id.fragment_authors)
    AuthorsFragment mAuthorsFragment;

    @ViewById
    SlidingPaneLayout fragment_container;

    @Override
    public void onAuthorSelected(long id) {
        // Capture the publications fragment from the activity layout
        mPubFragment.updatePublicationsView(id);
    }

    @AfterViews
    public void afterViews() {
        fragment_container.openPane();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Do not show menu in actionbar if authors are updating
        if (mAuthorsFragment != null) {
            return !mAuthorsFragment.isUpdating();
        }
        return true;
    }

    private BroadcastReceiver updateStatusReceiver;
    private BroadcastReceiver markAsReadReceiver;

    @Override
    protected void onResume() {
        if (updateStatusReceiver == null) {
            //AuthorsFragment is the callback here
            updateStatusReceiver = new UpdateStatusReceiver(mAuthorsFragment);
        }
        if (markAsReadReceiver == null) {
            markAsReadReceiver = new MarkedAsReadReceiver(mPubFragment, mAuthorsFragment);
        }
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(updateStatusReceiver, new UpdateStatusMessageFilter());
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(markAsReadReceiver, new MarkAsReadMessageFilter());
        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(updateStatusReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(markAsReadReceiver);
        super.onPause();
    }

}
