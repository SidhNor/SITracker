/*
 * Copyright 2016 Gleb Godonoga.
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


import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewStub;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.ui.PublicationDetailsActivity;
import com.andrada.sitracker.ui.fragment.adapters.NewPubsAdapter;
import com.andrada.sitracker.ui.widget.DividerItemDecoration;
import com.andrada.sitracker.ui.widget.ItemClickSupport;
import com.andrada.sitracker.util.AnalyticsHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

@EFragment(R.layout.fragment_newpubs)
public class NewPublicationsFragment extends BaseFragment {

    @ViewById(R.id.new_pubs_list)
    RecyclerView recyclerView;

    @ViewById
    View empty;

    @Bean
    NewPubsAdapter adapter;

    RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            if (adapter != null && empty != null) {
                if (adapter.getItemCount() > 0) {
                    recyclerView.setVisibility(View.VISIBLE);
                    empty.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    empty.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getBaseActivity().getActionBarToolbar().setTitle(getString(R.string.navdrawer_item_new_pubs));
        adapter.reloadNewPublications();
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        AnalyticsHelper.getInstance().sendView(Constants.GA_SCREEN_NEW_PUBLICATIONS);
    }

    @AfterViews
    void bindAdapter() {
        recyclerView.setHasFixedSize(true);
        adapter.registerAdapterDataObserver(dataObserver);
        recyclerView.setAdapter(adapter);
        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                Publication pub = adapter.getItemAt(position);
                if (pub != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            AppUriContract.buildPublicationUri(pub.getId()), getActivity(),
                            PublicationDetailsActivity.class);
                    ActivityCompat.startActivity(getActivity(), intent, null);
                }
            }
        });
    }
}
