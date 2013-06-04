package com.andrada.sitracker.phoneactivities;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.andrada.sitracker.R;
import com.andrada.sitracker.fragment.PublicationsFragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;

/**
 * Created by ggodonoga on 27/05/13.
 */
@EActivity(R.layout.publications_activity)
public class PublicationsActivity extends SherlockFragmentActivity {

    @FragmentById(R.id.fragment_publications)
    PublicationsFragment mPubFragment;

    @Extra(PublicationsFragment.ARG_ID)
    long mAuthorId;

    @AfterViews
    void updatePublications() {
        mPubFragment.updatePublicationsView(mAuthorId, this);
    }
}
