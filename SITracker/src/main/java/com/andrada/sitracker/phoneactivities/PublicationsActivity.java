package com.andrada.sitracker.phoneactivities;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.andrada.sitracker.R;
import com.andrada.sitracker.fragment.PublicationsFragment;

/**
 * Created by ggodonoga on 27/05/13.
 */
public class PublicationsActivity extends SherlockFragmentActivity{
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        setContentView(R.layout.publications_activity);

        if (extras != null) {
            // Take the info from the intent and deliver it to the fragment so it can update
            long authorId = extras.getLong(PublicationsFragment.ARG_ID);
            PublicationsFragment frag = (PublicationsFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_publications);
            frag.updatePublicationsView(authorId, this);
        }
    }
}
