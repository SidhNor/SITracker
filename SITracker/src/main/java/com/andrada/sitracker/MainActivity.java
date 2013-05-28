package com.andrada.sitracker;

import android.content.Intent;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.AuthorsFragment.OnAuthorSelectedListener;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog.OnAuthorAddedListener;
import com.andrada.sitracker.phoneactivities.PublicationsActivity;

public class MainActivity extends SherlockFragmentActivity implements
        OnAuthorSelectedListener, OnAuthorAddedListener {

    private boolean mDualFragments = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PublicationsFragment frag = (PublicationsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_publications);
        if (frag != null) mDualFragments = true;
    }
    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_add:
                AddAuthorDialog authorDialog = new AddAuthorDialog();
                authorDialog.setOnAuthorAddedListener(this);
                authorDialog.show(getSupportFragmentManager(),
                        Constants.DIALOG_ADD_AUTHOR);
                break;
            case R.id.action_refresh:

                break;

            default:
                break;
        }
        return true;
    }

    @Override
    public void onAuthorSelected(long id) {

        if (!mDualFragments) {
            // If showing only the AuthorsFragment, start the PublicationsActivity and
            // pass it the info about the selected item
            Intent intent = new Intent(this, PublicationsActivity.class);
            intent.putExtra(PublicationsFragment.ARG_ID, id);
            startActivity(intent);
        } else {
            // Capture the publications fragment from the activity layout
            PublicationsFragment pubsFrag = (PublicationsFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_publications);
            pubsFrag.updatePublicationsView(id, this);

        }
    }

    protected void updateAuthors() {
        AuthorsFragment authFrag = (AuthorsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (authFrag == null) {
            authFrag = (AuthorsFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_authors);
        }
        authFrag.updateViewAtPosition(authFrag.getSelectedItemPosition());
    }

    @Override
    public void onAuthorAdded() {
        updateAuthors();
    }

    @Override
    public void onProgressStarted() {
        //Show loading indicator
        AuthorsFragment authFrag = (AuthorsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_authors);
        if (authFrag != null) {
            authFrag.showAuthorLoadingProgress();
        }
    }
}
