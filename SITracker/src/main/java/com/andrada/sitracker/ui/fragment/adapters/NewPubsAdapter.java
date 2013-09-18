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

package com.andrada.sitracker.ui.fragment.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.HomeActivity;
import com.andrada.sitracker.ui.components.NewPubItemView;
import com.andrada.sitracker.ui.components.NewPubItemView_;
import com.andrada.sitracker.util.ImageLoader;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@EBean
public class NewPubsAdapter extends BaseAdapter {

    List<Publication> newPublications = new ArrayList<Publication>();

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    PublicationDao publicationDao;

    @RootContext
    Context context;

    ImageLoader mLoader;

    @Pref
    SIPrefs_ prefs;

    @AfterInject
    void initAdapter() {
        reloadPublications();
    }

    /**
     * Reloads new publications in background posting change-set notification to UI Thread
     */
    @Background
    public void reloadPublications() {
        try {
            boolean shouldShowImages = prefs.displayPubImages().get();
            if (shouldShowImages) {
                mLoader = ((HomeActivity) context).getImageLoaderInstance();
            } else {
                mLoader = null;
            }
            newPublications = publicationDao.getNewPublications();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        postDataSetChanged();
    }

    @UiThread
    protected void postDataSetChanged() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return newPublications.size();
    }

    @Override
    public Object getItem(int i) {
        return newPublications.get(i);
    }

    @Override
    public long getItemId(int i) {
        return newPublications.get(i).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NewPubItemView newPubItemView;
        if (convertView == null) {
            newPubItemView = NewPubItemView_.build(context);
        } else {
            newPubItemView = (NewPubItemView_) convertView;
        }
        newPubItemView.bind(newPublications.get(position), mLoader);
        return newPubItemView;
    }
}
