/*
 * Copyright 2013 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ExpandableListView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.fragment.adapters.PublicationsAdapter;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;

import de.greenrobot.event.EventBus;

@EFragment(R.layout.fragment_publications)
public class PublicationsFragment extends Fragment implements ExpandableListView.OnChildClickListener {

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
        if (mCurrentId == event.author.getId()) {
            //That means that we are viewing the current author
            //Just do a reload.
            updatePublicationsView(event.author.getId());
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
