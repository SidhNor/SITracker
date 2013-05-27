package com.andrada.sitracker;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.AuthorsFragment.OnAuthorSelectedListener;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog;
import com.andrada.sitracker.fragment.dialog.AddAuthorDialog.OnAuthorAddedListener;

public class MainActivity extends SherlockFragmentActivity implements
        OnAuthorSelectedListener, OnAuthorAddedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toast.makeText(getApplicationContext(), "TEST !", Toast.LENGTH_LONG).show();
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
        // The user selected the headline of an article from the
        // HeadlinesFragment

        // Capture the article fragment from the activity layout
        PublicationsFragment articleFrag = (PublicationsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_publications);

        if (articleFrag != null) {
            // If article frag is available, we're in two-pane layout...

            // Call a method in the ArticleFragment to update its content
            articleFrag.updateArticleView(id, this);

        } else {
            // If the frag is not available, we're in the one-pane layout and
            // must swap frags...

            // Create fragment and give it an argument for the selected article
            PublicationsFragment newFragment = new PublicationsFragment();
            Bundle args = new Bundle();
            args.putLong(PublicationsFragment.ARG_ID, id);
            newFragment.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();

            // Replace whatever is in the fragment_container view with this
            // fragment,
            // and add the transaction to the back stack so the user can
            // navigate back
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
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
}
