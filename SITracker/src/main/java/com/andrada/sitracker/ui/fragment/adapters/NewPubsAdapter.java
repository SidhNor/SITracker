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

package com.andrada.sitracker.ui.fragment.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.components.AuthorItemView_;
import com.andrada.sitracker.ui.components.NewPubItemView;
import com.andrada.sitracker.ui.components.NewPubItemView_;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@EBean
public class NewPubsAdapter extends BaseAdapter {

    private List<Publication> publications = new ArrayList<Publication>();

    @OrmLiteDao(helper = SiDBHelper.class)
    PublicationDao publicationsDao;

    @RootContext
    Context context;

    @Background
    public void reloadNewPublications() {
        List<Publication> pubs;
        try {
            pubs = publicationsDao.getNewPublications();
            postDataChanged(pubs);
        } catch (SQLException e) {
            //TODO do something about this error
            e.printStackTrace();
        }

    }

    @UiThread
    protected void postDataChanged(List<Publication> newPubs) {
        publications.clear();
        publications.addAll(newPubs);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return publications.size();
    }

    @Override
    public Object getItem(int position) {
        if (position >= 0 && position < publications.size()) {
            return publications.get(position);
        }
        return null;
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
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NewPubItemView newPubItemView;
        if (convertView == null) {
            newPubItemView = NewPubItemView_.build(context);
            //newPubItemView.setListener(this);
        } else {
            newPubItemView = (NewPubItemView) convertView;
        }
        if (position < publications.size()) {
            newPubItemView.bind(publications.get(position));
        }
        return newPubItemView;
    }
}
