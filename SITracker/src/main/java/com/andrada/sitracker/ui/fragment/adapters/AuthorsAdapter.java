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
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.ui.components.AuthorItemView;
import com.andrada.sitracker.ui.components.AuthorItemView_;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

@EBean
public class AuthorsAdapter extends BaseAdapter implements IsNewItemTappedListener {

    List<Author> authors = new ArrayList<Author>();
    long mNewAuthors;

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

    /**
     * Reloads authors in background posting changeset notification to UI Thread
     */
    @Background
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
            postDataSetChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    protected void postDataSetChanged() {
        notifyDataSetChanged();
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
        AuthorItemView authorsItemView;
        if (convertView == null) {
            authorsItemView = AuthorItemView_.build(context);
            authorsItemView.setListener(this);
        } else {
            authorsItemView = (AuthorItemView) convertView;
        }
        if (position < authors.size()) {
            authorsItemView.bind(authors.get(position), position == mSelectedItem);
        }
        return authorsItemView;
    }

    @Override
    @Background
    public void onIsNewItemTapped(View starButton) {
        Author auth = (Author) starButton.getTag();
        dismissAuthor(auth);
    }

    @Background
    public void markAuthorsRead(List<Long> authorsToMarkAsRead) {
        for (long authId : authorsToMarkAsRead) {
            dismissAuthor(this.getAuthorById(authId));
        }
    }

    /**
     * Should be called on background thread only
     *
     * @param auth Author to mark as read
     */
    private void dismissAuthor(Author auth) {
        if (auth != null) {
            auth.markRead();
            try {
                authorDao.update(auth);
                EventBus.getDefault().post(new AuthorMarkedAsReadEvent(auth));
            } catch (SQLException e) {
                EasyTracker.getTracker().sendException("Author mark as read: " + e.getMessage(), false);
            }
        }
    }

    @Background
    public void removeAuthors(List<Long> authorsToRemove) {
        try {
            for (int i = 0; i < authorsToRemove.size(); i++) {
                authorDao.removeAuthor(authorsToRemove.get(i));
            }
        } catch (SQLException e) {
            EasyTracker.getTracker().sendException("Author Remove thread", e, false);
        }

        boolean removingCurrentlySelected = false;
        for (Long anAuthorToRemove : authorsToRemove) {
            if (anAuthorToRemove == mSelectedAuthorId) {
                removingCurrentlySelected = true;
            }
            authors.remove(getAuthorById(anAuthorToRemove));
        }
        if (removingCurrentlySelected) {
            //Try select the first one
            setSelectedItem(getFirstAuthorId());
        } else {
            setSelectedItem(mSelectedAuthorId);
        }
        postDataSetChanged();
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

    private Author getAuthorById(long authorId) {
        for (int i = 0; i < authors.size(); i++) {
            if (authors.get(i).getId() == authorId) {
                return authors.get(i);
            }
        }
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
