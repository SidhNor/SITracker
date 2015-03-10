/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment;


import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.ui.PublicationDetailsActivity;
import com.andrada.sitracker.ui.fragment.adapters.NewPubsAdapter;
import com.andrada.sitracker.util.NavDrawerManager;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;

import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.widget.ListView;

@EFragment(R.layout.fragment_newpubs)
public class NewPublicationsFragment extends BaseListFragment {

    @ViewById(R.id.new_pubs_list)
    ListView list;

    @Bean
    NewPubsAdapter adapter;

    @Override
    public void onResume() {
        super.onResume();
        //Set title
        getBaseActivity().getDrawerManager()
                .pushNavigationalState(getString(R.string.navdrawer_item_new_pubs), true);

        adapter.reloadNewPublications();
    }

    @AfterViews
    void bindAdapter() {
        list.setAdapter(adapter);
    }

    @Override
    public ListView getScrollingView() {
        return list;
    }

    @Override
    public void setContentTopClearance(int clearance) {
        super.setContentTopClearance(clearance);
        if (list != null) {
            list.setPadding(list.getPaddingLeft(), clearance,
                    list.getPaddingRight(), list.getPaddingBottom());
            adapter.notifyDataSetChanged();
        }
    }

    @ItemClick(R.id.new_pubs_list)
    void listItemClick(Publication pub) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                AppUriContract.buildPublicationUri(pub.getId()), getActivity(),
                PublicationDetailsActivity.class);
        ActivityCompat.startActivity(getBaseActivity(), intent, null);
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return ViewCompat.canScrollVertically(list, -1);
    }

    @Override
    public int getSelfNavDrawerItem() {
        return NavDrawerManager.NAVDRAWER_ITEM_NEW_PUBS;
    }
}
