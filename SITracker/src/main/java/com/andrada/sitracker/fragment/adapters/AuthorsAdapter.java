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

package com.andrada.sitracker.fragment.adapters;

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.fragment.components.AuthorItemView;
import com.andrada.sitracker.fragment.components.AuthorItemView_;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;

import java.sql.SQLException;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by ggodonoga on 05/06/13.
 */

@EBean
public class AuthorsAdapter extends BaseAdapter implements IsNewItemTappedListener {

    List<Author> authors;
    long mNewAuthors;
    ListView listView = null;

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;

    @RootContext
    Context context;
    private int mSelectedItem = 0;

    private long mSelectedAuthorId = 0;

    @AfterInject
    void initAdapter() {
        reloadAuthors();
    }

    public void reloadAuthors() {
        try {
            int sortType = Integer.parseInt(
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .getString(Constants.AUTHOR_SORT_TYPE_KEY, "0"));
            if (sortType == 0) {
                authors = authorDao.getAllAuthorsSortedAZ();
            } else {
                authors = authorDao.getAllAuthorsSortedNew();
            }
            mNewAuthors = authorDao.getNewAuthorsCount();
            setSelectedItem(mSelectedAuthorId);
            notifyDataSetChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getCount() {
        return authors.size();
    }

    @Override
    public Object getItem(int position) {
        return authors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return authors.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (listView == null) {
            listView = (ListView) parent;
        }
        AuthorItemView authorsItemView;
        if (convertView == null) {
            authorsItemView = AuthorItemView_.build(context);
            authorsItemView.setListener(this);
        } else {
            authorsItemView = (AuthorItemView) convertView;
        }
        authorsItemView.bind(authors.get(position), position == mSelectedItem);
        return authorsItemView;
    }

    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        AuthorItemView authorsItemView = AuthorItemView_.build(context);
        authorsItemView.setListener(this);
        return authorsItemView;
    }

    public void bindView(View itemView, Context context, Author item) {
        AuthorItemView authorsItemView = (AuthorItemView) itemView;
        authorsItemView.bind(item, mSelectedAuthorId == item.getId());
    }

    @Override
    @Background
    public void onIsNewItemTapped(View starButton) {
        Author auth = (Author) starButton.getTag();
        if (auth != null) {
            auth.markRead();
            EventBus.getDefault().post(new AuthorMarkedAsReadEvent(auth.getId()));
       }
    }

    public void removeAuthors(List<Author> authorsToRemove) {
        try {
            authorDao.delete(authorsToRemove);
        } catch (SQLException e) {
            EasyTracker.getTracker().sendException("Author Remove thread", e, false);
        }

        boolean removingCurrentlySelected = false;
        for (Author anAuthorToRemove : authorsToRemove) {
            if (anAuthorToRemove.getId() == mSelectedAuthorId) {
                removingCurrentlySelected = true;
            }
            authors.remove(anAuthorToRemove);
        }
        if (removingCurrentlySelected) {
            //Try select the first one
            setSelectedItem(getFirstAuthorId());
        } else {
            setSelectedItem(mSelectedAuthorId);
        }
        notifyDataSetChanged();
    }

    public long getFirstAuthorId() {
        if (authors.size() > 0) {
            return authors.get(0).getId();
        }
        return -1;
    }

    public void setSelectedItem(long selectedItemId) {
        this.mSelectedAuthorId = selectedItemId;
        this.mSelectedItem = getItemPositionByAuthorId(selectedItemId);
    }

    public long getSelectedAuthorId() {
        return this.mSelectedAuthorId;
    }

    public Author getCurrentlySelectedAuthor() {
        if (mSelectedItem < authors.size() && mSelectedItem >= 0)
            return authors.get(mSelectedItem);
        return null;
    }

    private int getItemPositionByAuthorId(long authorId) {
        for (int i = 0; i < authors.size(); i++) {
            if (authors.get(i).getId() == authorId) {
                return i;
            }
        }
        return -1;
    }
}
