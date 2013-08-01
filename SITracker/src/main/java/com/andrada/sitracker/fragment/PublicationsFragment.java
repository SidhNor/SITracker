package com.andrada.sitracker.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.andrada.sitracker.R;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.fragment.adapters.PublicationsAdapter;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;

import de.greenrobot.event.EventBus;

@EFragment(R.layout.fragment_publications)
public class PublicationsFragment extends SherlockFragment implements ExpandableListView.OnChildClickListener {

    @Bean
    PublicationsAdapter adapter;

    @ViewById(R.id.publication_list)
    ExpandableListView mListView;

    @InstanceState
    long mCurrentId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @AfterViews
    void bindAdapter() {
        mListView.setAdapter(adapter);
        mListView.setOnChildClickListener(this);
        mListView.setOnItemLongClickListener(adapter);
        updatePublicationsView(mCurrentId);
    }

    public void updatePublicationsView(long id) {
        mCurrentId = id;
        adapter.reloadPublicationsForAuthorId(id);
    }

    public void onEvent(AuthorMarkedAsReadEvent event) {
        if (mCurrentId == event.authorId) {
            //That means that we are viewing the current author
            //Just do a reload.
            updatePublicationsView(event.authorId);
        }
    }

    public void onEvent(AuthorSelectedEvent event) {
        updatePublicationsView(event.authorId);
    }


    @Override
    public boolean onChildClick(ExpandableListView expandableListView,
                                View view, int groupPosition, int childPosition, long l) {
        //TODO redirect to other publication details fragment
        return false;
    }
}
