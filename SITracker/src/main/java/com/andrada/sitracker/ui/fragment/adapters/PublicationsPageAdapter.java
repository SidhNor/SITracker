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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.fragment.PublicationsFragment_;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@EBean
public class PublicationsPageAdapter extends SmartFragmentStatePagerAdapter {

    @OrmLiteDao(helper = SiDBHelper.class)
    AuthorDao authorDao;

    @Pref
    SIPrefs_ prefs;

    List<Author> authors = new ArrayList<Author>();

    private PublicationsPageAdapterListener listener;

    public PublicationsPageAdapter(Context context) {
        super(((Activity) context).getFragmentManager());
    }


    public void setListener(PublicationsPageAdapterListener listener) {
        this.listener = listener;
    }

    @Background
    public void reloadAuthors() {
        try {
            int sortType = Integer.parseInt(prefs.authorsSortType().get());
            List<Author> newList;
            if (sortType == 0) {
                newList = authorDao.getAllAuthorsSortedAZ();
            } else {
                newList = authorDao.getAllAuthorsSortedNew();
            }
            postDataSetChanged(newList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Author getItemDSForPosition(int position) {
        return authors.get(position);
    }


    public int getItemPositionForId(long authorId) {
        for (int i = 0; i < authors.size(); i++) {
            if (authors.get(i).getId() == authorId) {
                return i;
            }
        }
        return -1;
    }

    @UiThread
    protected void postDataSetChanged(List<Author> newAuthors) {
        authors.clear();
        authors.addAll(newAuthors);
        notifyDataSetChanged();
        if (listener != null) {
            listener.pagesLoaded();
        }
    }

    @AfterInject
    void afterInject() {
        reloadAuthors();
    }

    @Override
    public Fragment getItem(int position) {
        Author auth = authors.get(position);
        return PublicationsFragment_.builder().authorName(auth.getName()).activeAuthorId(auth.getId()).build();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Author auth = authors.get(position);
        return auth.getName();
    }

    @Override
    public int getCount() {
        return authors.size();
    }

    public interface PublicationsPageAdapterListener {
        void pagesLoaded();
    }
}
