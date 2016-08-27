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

package com.andrada.sitracker.ui.fragment.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.components.NewPubItemView;
import com.andrada.sitracker.ui.components.NewPubItemView_;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.ormlite.annotations.OrmLiteDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EBean
public class NewPubsAdapter extends RecyclerView.Adapter<NewPubsAdapter.ViewHolder> implements IsNewItemTappedListener {

    private List<Publication> publications = new ArrayList<Publication>();

    @OrmLiteDao(helper = SiDBHelper.class)
    PublicationDao publicationsDao;

    @RootContext
    Context context;

    @Pref
    SIPrefs_ prefs;

    boolean shouldShowImages;

    private OnItemClickListener listener;

    @Background
    public void reloadNewPublications() {
        List<Publication> pubs;
        shouldShowImages = prefs.displayPubImages().get();
        try {
            pubs = publicationsDao.getNewPublications();
            Iterator<Publication> iter = pubs.iterator();
            while (iter.hasNext()) {
                if (iter.next().getAuthor() == null) {
                    iter.remove();
                }
            }
            postDataChanged(pubs);
        } catch (SQLException e) {
            //TODO do something about this error
            e.printStackTrace();
        }

    }

    @Background
    public void markAllPublicationsAsRead() {
        try {
            List<Publication> pubs = publicationsDao.getNewPublications();
            for (Publication pub : pubs) {
                publicationsDao.markPublicationRead(pub);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearAll() {
        publications.clear();
        notifyDataSetChanged();
    }

    @UiThread
    protected void postDataChanged(List<Publication> newPubs) {
        publications.clear();
        publications.addAll(newPubs);
        notifyDataSetChanged();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(NewPubItemView_.build(context), listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position < publications.size()) {
            holder.view.bind(publications.get(position), shouldShowImages);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < publications.size()) {
            Publication pub = publications.get(position);
            return pub.getId();
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return publications.size();
    }

    @Override
    public void onIsNewItemTapped(View checkBox) {
        //TODO make item not new, reload stuff
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public Publication getItemAt(int position) {
        if (position >= 0 && position < publications.size()) {
            return publications.get(position);
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        NewPubItemView view;

        public ViewHolder(final NewPubItemView itemView, final OnItemClickListener listener) {
            super(itemView);
            view = itemView;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(itemView.getCurrentPublicationId());
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Long publicationId);
    }
}
