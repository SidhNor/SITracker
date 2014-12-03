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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.exceptions.SharePublicationException;
import com.andrada.sitracker.ui.PublicationDetailsActivity;
import com.andrada.sitracker.ui.fragment.adapters.PublicationsAdapter;
import com.andrada.sitracker.ui.fragment.adapters.PublicationsAdapter_;
import com.andrada.sitracker.util.ShareHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.andrada.sitracker.util.LogUtils.LOGI;

@EFragment(R.layout.fragment_publications)
@OptionsMenu(R.menu.publications_menu)
public class PublicationsFragment extends BaseListFragment implements
        ExpandableListView.OnChildClickListener,
        PublicationsAdapter.PublicationShareAttemptListener {

    @Bean
    PublicationsAdapter adapter;

    @ViewById(R.id.publication_list)
    ExpandableListView mListView;

    @FragmentArg("currentAuthorId")
    @InstanceState
    long activeAuthorId = -1;

    @FragmentArg("authorName")
    String authorName = "";

    @Pref
    SIPrefs_ prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGI("SITracker", "PublicationsFragment - OnCreate");
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        //getBaseActivity().getDrawerManager().pushNavigationalState(authorName, false);
        int priority = 3;
        //PublicationsFragment has a higher priority then SiMainActivity
        EventBus.getDefault().register(this, priority);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOGI("SITracker", "PublicationsFragment - OnDestroy");
    }

    @AfterViews
    void bindAdapter() {
        ((PublicationsAdapter_) adapter).rebind(getActivity());
        mListView.setAdapter(adapter);
        adapter.setShareListener(this);
        mListView.setOnChildClickListener(this);
        mListView.setOnItemLongClickListener(adapter);
        updatePublicationsView(activeAuthorId);
    }

    public void updatePublicationsView(long id) {
        activeAuthorId = id;
        adapter.reloadPublicationsForAuthorId(id);
    }

    public void onEvent(@NotNull AuthorMarkedAsReadEvent event) {
        if (activeAuthorId == event.author.getId()) {
            //That means that we are viewing the current author
            //Just do a reload.
            updatePublicationsView(event.author.getId());
        }
    }

    @UiThread
    public void stopProgressAfterShare(boolean success, String errorMessage, long id) {
        //Stop loading progress in adapter
        adapter.stopProgressOnPublication(id, success);
        if (!success) {
            Style.Builder alertStyle = new Style.Builder()
                    .setTextAppearance(android.R.attr.textAppearanceLarge)
                    .setPaddingInPixels(25);
            alertStyle.setBackgroundColorValue(Style.holoRedLight);
            Crouton.makeText(getActivity(), errorMessage, alertStyle.build()).show();
        }
    }

    /*
        public void onEvent(@NotNull AuthorSelectedEvent event) {
            EventBus.getDefault().cancelEventDelivery(event);
            updatePublicationsView(event.authorId);
        }
    */
    @Override
    public boolean onChildClick(ExpandableListView expandableListView,
                                View view, int groupPosition, int childPosition, long l) {
        Publication pub = (Publication) adapter.getChild(groupPosition, childPosition);

        Intent intent = new Intent(Intent.ACTION_VIEW,
                AppUriContract.buildPublicationUri(pub.getId()), getActivity(), PublicationDetailsActivity.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getBaseActivity(),
                new Pair<View, String>(view.findViewById(R.id.item_title), "publicationTitle"),
                new Pair<View, String>(view.findViewById(R.id.item_description), "publicationAbstract"));
        ActivityCompat.startActivity(getBaseActivity(), intent, options.toBundle());
        return true;
    }

    @Override
    @Background
    public void publicationShare(@NotNull Publication pub, boolean forceDownload) {

        int errorMessage = -1;
        try {
            Intent intent = ShareHelper.fetchPublication(getActivity(), pub, prefs.downloadFolder().get(), forceDownload);
            //Open only if our app is actually running
            if (getActivity() != null) {
                getActivity().startActivity(intent);
            }
        } catch (SharePublicationException e) {
            switch (e.getError()) {
                case COULD_NOT_PERSIST:
                    errorMessage = R.string.publication_error_save;
                    break;
                case STORAGE_NOT_ACCESSIBLE_FOR_PERSISTANCE:
                    errorMessage = R.string.publication_error_storage;
                    break;
                case ERROR_UNKOWN:
                    errorMessage = R.string.publication_error_unknown;
                    break;
                case COULD_NOT_LOAD:
                    errorMessage = R.string.cannot_download_publication;
                    break;
                case WRONG_PUBLICATION_URL:
                    errorMessage = R.string.publication_error_url;
                    break;
                default:
                    break;
            }
        } finally {
            String msg = "";
            if (errorMessage != -1 && getActivity() != null) {
                msg = getActivity().getResources().getString(errorMessage);
            }
            stopProgressAfterShare(errorMessage == -1, msg, pub.getId());

        }
    }

    @Override
    public void setContentTopClearance(int clearance) {
        super.setContentTopClearance(clearance);
        if (mListView != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mListView.getLayoutParams();
            params.setMargins(0, clearance, 0, 0);
            /*mListView.setPadding(mListView.getPaddingLeft(), clearance,
                    mListView.getPaddingRight(), mListView.getPaddingBottom());*/
            adapter.notifyDataSetChanged();
        }
    }
}
