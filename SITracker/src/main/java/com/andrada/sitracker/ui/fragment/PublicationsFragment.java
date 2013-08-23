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

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ExpandableListView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.exceptions.SharePublicationException;
import com.andrada.sitracker.ui.fragment.adapters.PublicationsAdapter;
import com.andrada.sitracker.util.ShareHelper;
import com.andrada.sitracker.util.UIUtils;
import com.github.kevinsawicki.http.HttpRequest;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@EFragment(R.layout.fragment_publications)
public class PublicationsFragment extends Fragment implements ExpandableListView.OnChildClickListener, PublicationsAdapter.PublicationShareAttemptListener {

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
        adapter.setShareListener(this);
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

    @UiThread
    public void stopProgressAfterShare(boolean success, String errorMessage, long id) {
        //Stop loading progress in adapter
        adapter.stopProgressOnPublication(id);
        if (!success) {
            Crouton.showText(getActivity(), errorMessage, Style.ALERT);
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

    @Override
    public void publicationShare(Publication pub, boolean forceDownload) {
        HttpRequest request;
        String pubUrl = pub.getUrl();
        File file = ShareHelper.getPublicationStorageFile(getActivity(), UIUtils.hashKeyForDisk(pubUrl));

        String errorMessage = "";
        boolean shareResult = true;

        try {
            if (forceDownload || !file.exists()) {
                URL authorURL = new URL(pubUrl);
                request = HttpRequest.get(authorURL);
                if (request.code() == 200) {
                    String content = request.body();
                    if (file == null) {
                        throw new SharePublicationException(
                                SharePublicationException.SharePublicationErrors.STORAGE_NOT_ACCESSIBLE_FOR_PERSISTANCE);
                    }
                    boolean result = ShareHelper.saveHtmlPageToFile(file, content, request.charset());
                    if (!result) {
                        throw new SharePublicationException(
                                SharePublicationException.SharePublicationErrors.COULD_NOT_PERSIST);
                    }
                    getActivity().startActivity(ShareHelper.getSharePublicationIntent(Uri.fromFile(file)));
                } else {
                    throw new SharePublicationException(
                            SharePublicationException.SharePublicationErrors.COULD_NOT_LOAD);
                }
            } else {
                getActivity().startActivity(ShareHelper.getSharePublicationIntent(Uri.fromFile(file)));
            }
        } catch (MalformedURLException e) {
            errorMessage = getActivity().getResources().getString(R.string.publication_error_url);
            shareResult = false;
        } catch (HttpRequest.HttpRequestException e) {
            errorMessage = getActivity().getResources().getString(R.string.cannot_download_publication);
            shareResult = false;
        } catch (SharePublicationException e) {
            switch (e.getError()) {
                case COULD_NOT_PERSIST:
                    errorMessage = getActivity().getResources().getString(R.string.publication_error_save);
                    break;
                case STORAGE_NOT_ACCESSIBLE_FOR_PERSISTANCE:
                    errorMessage = getActivity().getResources().getString(R.string.publication_error_storage);
                    break;
                case ERROR_UNKOWN:
                    errorMessage = getActivity().getResources().getString(R.string.publication_error_unknown);
                    break;
                case COULD_NOT_LOAD:
                    errorMessage = getActivity().getResources().getString(R.string.cannot_download_publication);
                    break;
            }
            shareResult = false;
        } finally {
            stopProgressAfterShare(shareResult, errorMessage, pub.getId());
        }
    }
}
