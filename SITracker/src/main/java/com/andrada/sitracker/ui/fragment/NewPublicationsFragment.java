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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.analytics.ViewNewPublications;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.ui.PublicationDetailsActivity;
import com.andrada.sitracker.ui.fragment.adapters.NewPubsAdapter;
import com.andrada.sitracker.ui.widget.DividerItemDecoration;
import com.andrada.sitracker.analytics.AnalyticsManager;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

@EFragment(R.layout.fragment_newpubs)
@OptionsMenu(R.menu.new_pubs_menu)
public class NewPublicationsFragment extends BaseFragment implements NewPubsAdapter.OnItemClickListener {

    @ViewById(R.id.new_pubs_list)
    RecyclerView recyclerView;

    @ViewById
    View empty;

    @Bean
    NewPubsAdapter adapter;

    RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            handleEmptyView();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            handleEmptyView();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            handleEmptyView();
        }
    };

    private void handleEmptyView() {
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

    @Override
    public void onResume() {
        super.onResume();
        getBaseActivity().getActionBarToolbar().setTitle(getString(R.string.navdrawer_item_new_pubs));
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        adapter.registerAdapterDataObserver(dataObserver);
        adapter.reloadNewPublications();
        AnalyticsManager.getInstance().logEvent(new ViewNewPublications());
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.unregisterAdapterDataObserver(dataObserver);
    }

    @AfterViews
    void bindAdapter() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(Long publicationId) {
        if (publicationId != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    AppUriContract.buildPublicationUri(publicationId), getActivity(),
                    PublicationDetailsActivity.class);
            ActivityCompat.startActivity(getActivity(), intent, null);
        }
    }

    //region Menu item tap handlers
    @OptionsItem(R.id.action_sweep_all)
    void menuSweepAlSelected() {
        if (adapter.getItemCount() == 0) {
            return;
        }
        adapter.clearAll();
        Snackbar.make(getActivity().findViewById(R.id.main_content), R.string.all_read, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        switch (event) {
                            case Snackbar.Callback.DISMISS_EVENT_ACTION:
                                adapter.reloadNewPublications();
                                break;
                            case Snackbar.Callback.DISMISS_EVENT_TIMEOUT:
                                adapter.markAllPublicationsAsRead();
                                break;
                        }
                    }
                })
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //UNDO stuff
                        //Handled in another callback
                    }
                }).show();
    }

    //endregion
}
